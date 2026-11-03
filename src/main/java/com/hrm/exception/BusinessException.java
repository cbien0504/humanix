package com.hrm.exception;

public class BusinessException extends AppException {

    public BusinessException(String message) {
        super(409, "BUSINESS_RULE_VIOLATION", message, null);
    }

    public BusinessException(String message, Throwable cause) {
        super(409, "BUSINESS_RULE_VIOLATION", message, cause);
    }
}
