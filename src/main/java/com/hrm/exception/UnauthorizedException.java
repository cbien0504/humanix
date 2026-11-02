package com.hrm.exception;

/** The caller is not authenticated (HTTP 401). */
public class UnauthorizedException extends AppException {

    public UnauthorizedException(String message) {
        super(401, "UNAUTHORIZED", message, null);
    }
}
