package com.hrm.exception;

public class DataAccessException extends AppException {

    public DataAccessException(String message, Throwable cause) {
        super(500, "DATA_ACCESS_ERROR", message, cause);
    }

    protected DataAccessException(int status, String code, String message, Throwable cause) {
        super(status, code, message, cause);
    }
}
