package com.hrm.dao.jdbc;

import com.hrm.dao.TransactionManager;
import com.hrm.exception.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Thin JDBC template used by every DAO.
 * <ul>
 *   <li>Always uses {@link PreparedStatement}/{@link CallableStatement} with bound parameters.</li>
 *   <li>Closes resources (try-with-resources) and translates {@link SQLException} via
 *       {@link SqlErrorTranslator}.</li>
 *   <li>Implements {@link TransactionManager}: inside {@link #inTransaction} all DAO calls of
 *       the current thread share one connection.</li>
 *   <li>Before writes, sets {@code SESSION_CONTEXT('app_user')} so database triggers can record
 *       which application user changed the data (salary history, candidate pipeline).</li>
 *   <li>Logs every statement at DEBUG and statements slower than the threshold at WARN.</li>
 * </ul>
 */
public class JdbcExecutor implements TransactionManager {

    private static final Logger log = LoggerFactory.getLogger(JdbcExecutor.class);
    private static final String SET_APP_USER = "EXEC sys.sp_set_session_context @key = N'app_user', @value = ?";

    private final DataSource dataSource;
    private final Supplier<String> auditUser;
    private final long slowQueryMs;
    private final SqlErrorTranslator translator = new SqlErrorTranslator();
    private final ThreadLocal<Connection> txConnection = new ThreadLocal<>();

    @FunctionalInterface
    public interface ConnectionCallback<T> {
        T doInConnection(Connection connection) throws SQLException;
    }

    public JdbcExecutor(DataSource dataSource, Supplier<String> auditUser, long slowQueryMs) {
        this.dataSource = dataSource;
        this.auditUser = auditUser;
        this.slowQueryMs = slowQueryMs;
    }

    // ------------------------------------------------------------------ queries

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        return run(sql, false, con -> {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    List<T> rows = new ArrayList<>();
                    while (rs.next()) {
                        rows.add(mapper.map(rs));
                    }
                    return rows;
                }
            }
        });
    }

    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = query(sql, mapper, params);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** First column of the first row, or {@code null} when there is no row. */
    public <T> T queryScalar(String sql, Class<T> type, Object... params) {
        return queryOne(sql, rs -> rs.getObject(1, type), params).orElse(null);
    }

    public long count(String sql, Object... params) {
        Number n = queryOne(sql, rs -> (Number) rs.getObject(1), params).orElse(0);
        return n.longValue();
    }

    // ------------------------------------------------------------------ writes

    public int update(String sql, Object... params) {
        return run(sql, true, con -> {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                bind(ps, params);
                return ps.executeUpdate();
            }
        });
    }

    /** Executes an INSERT and returns the generated identity value. */
    public long insert(String sql, Object... params) {
        return run(sql, true, con -> {
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, params);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("INSERT did not return a generated key");
                    }
                    return keys.getLong(1);
                }
            }
        });
    }

    public int[] batch(String sql, List<Object[]> rows) {
        if (rows.isEmpty()) {
            return new int[0];
        }
        return run(sql, true, con -> {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (Object[] row : rows) {
                    bind(ps, row);
                    ps.addBatch();
                }
                return ps.executeBatch();
            }
        });
    }

    /** Calls a stored procedure, e.g. {@code {call dbo.sp_X(?, ?)}}, and maps its first result set. */
    public <T> List<T> call(String call, RowMapper<T> mapper, Object... params) {
        return run(call, true, con -> {
            try (CallableStatement cs = con.prepareCall(call)) {
                bind(cs, params);
                boolean hasResult = cs.execute();
                while (!hasResult && cs.getUpdateCount() != -1) {
                    hasResult = cs.getMoreResults();
                }
                List<T> rows = new ArrayList<>();
                if (hasResult) {
                    try (ResultSet rs = cs.getResultSet()) {
                        while (rs.next()) {
                            rows.add(mapper.map(rs));
                        }
                    }
                }
                return rows;
            }
        });
    }

    /** Escape hatch for multi-statement work on one connection. */
    public <T> T execute(String description, boolean write, ConnectionCallback<T> callback) {
        return run(description, write, callback);
    }

    // ------------------------------------------------------------------ transactions

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        if (txConnection.get() != null) {
            return work.get();                       // join the outer transaction
        }
        Connection con;
        try {
            con = dataSource.getConnection();
        } catch (SQLException e) {
            throw translator.translate(e, "BEGIN TRANSACTION");
        }
        try {
            con.setAutoCommit(false);
            setAppUser(con);
            txConnection.set(con);
            T result = work.get();
            con.commit();
            return result;
        } catch (SQLException e) {
            rollbackQuietly(con);
            throw translator.translate(e, "TRANSACTION");
        } catch (RuntimeException | Error e) {
            rollbackQuietly(con);
            throw e;
        } finally {
            txConnection.remove();
            try {
                con.setAutoCommit(true);
                con.close();
            } catch (SQLException e) {
                log.warn("Could not release transactional connection: {}", e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ internals

    private <T> T run(String sql, boolean write, ConnectionCallback<T> callback) {
        long start = System.nanoTime();
        Connection tx = txConnection.get();
        try {
            if (tx != null) {
                return callback.doInConnection(tx);
            }
            try (Connection con = dataSource.getConnection()) {
                if (write) {
                    setAppUser(con);
                }
                return callback.doInConnection(con);
            }
        } catch (SQLException e) {
            RuntimeException translated = translator.translate(e, sql);
            if (translated instanceof AppException app && app.getHttpStatus() >= 500) {
                log.error("SQL failed [{}] {}: {}", e.getErrorCode(), flatten(sql), e.getMessage());
            } else {
                log.debug("SQL rejected [{}] {}: {}", e.getErrorCode(), flatten(sql), e.getMessage());
            }
            throw translated;
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            if (ms >= slowQueryMs) {
                log.warn("Slow SQL ({} ms): {}", ms, flatten(sql));
            } else if (log.isDebugEnabled()) {
                log.debug("SQL ({} ms): {}", ms, flatten(sql));
            }
        }
    }

    private void setAppUser(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SET_APP_USER)) {
            String user = auditUser.get();
            ps.setString(1, user == null ? "system" : user);
            ps.execute();
        }
    }

    static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            int index = i + 1;
            if (p == null) {
                ps.setObject(index, null);
            } else if (p instanceof String s) {
                ps.setString(index, s);
            } else if (p instanceof Integer n) {
                ps.setInt(index, n);
            } else if (p instanceof Long n) {
                ps.setLong(index, n);
            } else if (p instanceof BigDecimal d) {
                ps.setBigDecimal(index, d);
            } else if (p instanceof Boolean b) {
                ps.setBoolean(index, b);
            } else if (p instanceof Instant instant) {
                ps.setObject(index, LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
            } else if (p instanceof Enum<?> e) {
                ps.setString(index, e.name());
            } else {
                ps.setObject(index, p);    // LocalDate, LocalDateTime, LocalTime, Short, Double…
            }
        }
    }

    private static void rollbackQuietly(Connection con) {
        try {
            con.rollback();
        } catch (SQLException e) {
            log.warn("Rollback failed: {}", e.getMessage());
        }
    }

    private static String flatten(String sql) {
        return sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
    }
}
