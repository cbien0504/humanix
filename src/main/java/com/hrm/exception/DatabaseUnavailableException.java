package com.hrm.exception;

public class DatabaseUnavailableException extends DataAccessException {

    public DatabaseUnavailableException(Throwable cause) {
        super(503, "DATABASE_UNAVAILABLE",
                "Hệ thống tạm thời không kết nối được cơ sở dữ liệu. Vui lòng thử lại sau ít phút.", cause);
    }
}
