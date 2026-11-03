package com.hrm.exception;

public abstract class AppException extends RuntimeException {

    private final int httpStatus;
    private final String errorCode;

    protected AppException(int httpStatus, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
