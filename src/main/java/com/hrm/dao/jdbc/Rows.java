package com.hrm.dao.jdbc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Null-safe {@link ResultSet} accessors using java.time types. */
public final class Rows {

    private Rows() {
    }

    public static Integer integer(ResultSet rs, String column) throws SQLException {
        int v = rs.getInt(column);
        return rs.wasNull() ? null : v;
    }

    public static Long longValue(ResultSet rs, String column) throws SQLException {
        long v = rs.getLong(column);
        return rs.wasNull() ? null : v;
    }

    public static BigDecimal decimal(ResultSet rs, String column) throws SQLException {
        return rs.getBigDecimal(column);
    }

    public static LocalDate date(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDate.class);
    }

    public static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDateTime.class);
    }

    /** Reads a DATETIME2 column stored in UTC (SYSUTCDATETIME()) as an {@link Instant}. */
    public static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime utc = rs.getObject(column, LocalDateTime.class);
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC);
    }

    public static boolean bool(ResultSet rs, String column) throws SQLException {
        return rs.getBoolean(column);
    }
}
