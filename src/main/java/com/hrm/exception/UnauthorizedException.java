package com.hrm.exception;

public class UnauthorizedException extends AppException {

    public UnauthorizedException(String message) {
        super(401, "UNAUTHORIZED", message, null);
    }
}
