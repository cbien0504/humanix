package com.hrm.exception;

/** The database cannot be reached (network down, server stopped, pool exhausted) – HTTP 503. */
public class DatabaseUnavailableException extends DataAccessException {

    public DatabaseUnavailableException(Throwable cause) {
        super(503, "DATABASE_UNAVAILABLE",
                "Hệ thống tạm thời không kết nối được cơ sở dữ liệu. Vui lòng thử lại sau ít phút.", cause);
    }
}
