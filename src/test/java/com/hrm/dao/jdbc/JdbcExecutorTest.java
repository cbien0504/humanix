package com.hrm.dao.jdbc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hrm.exception.BusinessException;
import com.hrm.exception.DataAccessException;
import com.hrm.exception.DataIntegrityException;
import com.hrm.exception.DatabaseUnavailableException;
import com.hrm.model.EmployeeStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcExecutorTest {

    private static final String SET_APP_USER = "EXEC sys.sp_set_session_context @key = N'app_user', @value = ?";

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection con;
    @Mock
    private PreparedStatement ps;
    @Mock
    private PreparedStatement contextStatement;
    @Mock
    private CallableStatement cs;
    @Mock
    private ResultSet rs;

    private JdbcExecutor jdbc;

    @BeforeEach
    void setUp() throws SQLException {
        jdbc = new JdbcExecutor(dataSource, () -> "hr", 60_000);
        lenient().when(dataSource.getConnection()).thenReturn(con);
        lenient().when(con.prepareStatement(SET_APP_USER)).thenReturn(contextStatement);
    }

    @Test
    void queriesBindEveryParameterTypeAndCloseTheirResources() throws SQLException {
        when(con.prepareStatement("SELECT x")).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString(1)).thenReturn("a", "b");

        List<String> rows = jdbc.query("SELECT x", r -> r.getString(1), "text", 5, 7L, new BigDecimal("1.50"), true,
                Instant.parse("2026-11-02T01:30:00Z"), EmployeeStatus.ACTIVE, LocalDate.of(2026, 11, 2), null);

        assertThat(rows).containsExactly("a", "b");
        verify(ps).setString(1, "text");
        verify(ps).setInt(2, 5);
        verify(ps).setLong(3, 7L);
        verify(ps).setBigDecimal(4, new BigDecimal("1.50"));
        verify(ps).setBoolean(5, true);
        verify(ps).setObject(6, LocalDateTime.of(2026, 11, 2, 1, 30));
        verify(ps).setString(7, "ACTIVE");
        verify(ps).setObject(8, LocalDate.of(2026, 11, 2));
        verify(ps).setObject(9, null);
        verify(rs).close();
        verify(ps).close();
        verify(con).close();
        verify(con, never()).prepareStatement(SET_APP_USER);
    }

    @Test
    void writesTellTheTriggersWhoMadeTheChange() throws SQLException {
        when(con.prepareStatement("UPDATE dbo.Employees SET BaseSalary = ? WHERE EmployeeId = ?")).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        assertThat(jdbc.update("UPDATE dbo.Employees SET BaseSalary = ? WHERE EmployeeId = ?", new BigDecimal("18000000"), 16L))
                .isEqualTo(1);

        InOrder order = inOrder(contextStatement, ps);
        order.verify(contextStatement).setString(1, "hr");
        order.verify(contextStatement).execute();
        order.verify(ps).executeUpdate();
    }

    @Test
    void insertReturnsTheIdentityValue() throws SQLException {
        when(con.prepareStatement("INSERT x", Statement.RETURN_GENERATED_KEYS)).thenReturn(ps);
        when(ps.getGeneratedKeys()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong(1)).thenReturn(42L);

        assertThat(jdbc.insert("INSERT x", "EMP0100")).isEqualTo(42L);
    }

    @Test
    void anInsertWithoutGeneratedKeyIsAnError() throws SQLException {
        when(con.prepareStatement("INSERT x", Statement.RETURN_GENERATED_KEYS)).thenReturn(ps);
        when(ps.getGeneratedKeys()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertThatThrownBy(() -> jdbc.insert("INSERT x")).isInstanceOf(DataAccessException.class);
    }

    @Test
    void storedProcedureRowCountsBeforeTheResultSetAreSkipped() throws SQLException {
        when(con.prepareCall("{call dbo.sp_CalculateMonthlyPayroll(?, ?, ?)}")).thenReturn(cs);
        when(cs.execute()).thenReturn(false);
        when(cs.getUpdateCount()).thenReturn(36);
        when(cs.getMoreResults()).thenReturn(true);
        when(cs.getResultSet()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getLong("PeriodId")).thenReturn(9L);

        List<Long> ids = jdbc.call("{call dbo.sp_CalculateMonthlyPayroll(?, ?, ?)}", r -> r.getLong("PeriodId"), 2026, 10, "hr");

        assertThat(ids).containsExactly(9L);
        verify(cs).setInt(1, 2026);
        verify(cs).setString(3, "hr");
    }

    @Test
    void anEmptyBatchDoesNotOpenAConnection() throws SQLException {
        assertThat(jdbc.batch("INSERT x VALUES (?)", List.of())).isEmpty();
        verify(dataSource, never()).getConnection();
    }

    @Nested
    class Transactions {

        @Test
        void allStatementsShareOneConnectionAndCommitOnce() throws SQLException {
            when(con.prepareStatement("UPDATE a")).thenReturn(ps);

            jdbc.inTransaction(() -> {
                jdbc.update("UPDATE a");
                jdbc.update("UPDATE a");
                jdbc.inTransaction(() -> jdbc.update("UPDATE a"));
            });

            verify(dataSource, times(1)).getConnection();
            verify(ps, times(3)).executeUpdate();
            InOrder order = inOrder(con, contextStatement);
            order.verify(con).setAutoCommit(false);
            order.verify(contextStatement).execute();
            order.verify(con).commit();
            order.verify(con).setAutoCommit(true);
            order.verify(con).close();
            verify(con, never()).rollback();
        }

        @Test
        void aFailureRollsBackAndIsRethrownUnchanged() throws SQLException {
            BusinessException failure = new BusinessException("Kỳ lương đã khóa.");

            assertThatThrownBy(() -> jdbc.inTransaction(() -> {
                throw failure;
            })).isSameAs(failure);

            verify(con).rollback();
            verify(con, never()).commit();
            verify(con).close();
        }
    }

    @Nested
    class Errors {

        @Test
        void duplicateKeyIsTranslatedAndTheConnectionStillClosed() throws SQLException {
            when(con.prepareStatement("INSERT x", Statement.RETURN_GENERATED_KEYS)).thenReturn(ps);
            when(ps.executeUpdate()).thenThrow(new SQLException("Violation of UNIQUE KEY constraint 'UQ_Employees_EmployeeCode'. "
                    + "Cannot insert duplicate key in object 'dbo.Employees'.", "23000", 2627));

            assertThatThrownBy(() -> jdbc.insert("INSERT x", "EMP0001"))
                    .isInstanceOfSatisfying(DataIntegrityException.class, e -> assertThat(e.getField()).isEqualTo("employeeCode"));
            verify(ps).close();
            verify(con).close();
        }

        @Test
        void poolTimeoutBecomesDatabaseUnavailable() throws SQLException {
            when(dataSource.getConnection()).thenThrow(
                    new SQLTransientConnectionException("hrm-pool - Connection is not available, request timed out after 5000ms."));

            assertThatThrownBy(() -> jdbc.count("SELECT COUNT(*) FROM dbo.Employees"))
                    .isInstanceOfSatisfying(DatabaseUnavailableException.class, e -> assertThat(e.getHttpStatus()).isEqualTo(503));
            assertThatThrownBy(() -> jdbc.inTransaction(() -> 1)).isInstanceOf(DatabaseUnavailableException.class);
            verifyNoInteractions(con);
        }

        @Test
        void countReadsTheFirstColumn() throws SQLException {
            when(con.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true, false);
            when(rs.getObject(1)).thenReturn(36);

            assertThat(jdbc.count("SELECT COUNT(*) FROM dbo.Employees")).isEqualTo(36L);
        }
    }

    @Nested
    class Logging {

        private final ListAppender<ILoggingEvent> events = new ListAppender<>();
        private final Logger logger = (Logger) LoggerFactory.getLogger(JdbcExecutor.class);
        private JdbcExecutor everythingIsSlow;

        @BeforeEach
        void attach() {
            events.start();
            logger.addAppender(events);
            everythingIsSlow = new JdbcExecutor(dataSource, () -> "hr", 0);
        }

        @AfterEach
        void detach() {
            logger.detachAppender(events);
        }

        @Test
        void slowStatementsAreWarned() throws SQLException {
            when(con.prepareStatement("SELECT 1")).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            everythingIsSlow.query("SELECT 1", r -> 1);

            assertThat(events.list).extracting(ILoggingEvent::getFormattedMessage).anyMatch(m -> m.startsWith("Slow SQL"));
        }

        @Test
        void waitingForAnUnavailableDatabaseIsNeitherAnErrorNorASlowQueryAtThisLevel() throws SQLException {
            when(dataSource.getConnection()).thenThrow(new SQLTransientConnectionException("request timed out after 5000ms"));

            assertThatThrownBy(() -> everythingIsSlow.count("SELECT 1")).isInstanceOf(DatabaseUnavailableException.class);
            assertThat(events.list).noneMatch(e -> e.getLevel().isGreaterOrEqual(Level.WARN));
        }
    }
}
