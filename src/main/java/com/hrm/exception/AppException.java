package com.hrm.exception;

/**
 * Base class of every exception the application raises on purpose.
 * <p>
 * Each subclass carries the HTTP status and a stable machine-readable error code, so the
 * central {@code ErrorHandler} can turn any of them into a consistent JSON body or error page
 * without {@code instanceof} chains in controllers.
 */
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
