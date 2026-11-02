package com.hrm.exception;

/**
 * A business rule forbids the operation in the current state (HTTP 409), e.g. approving an
 * already rejected leave request or recalculating an approved payroll period.
 */
public class BusinessException extends AppException {

    public BusinessException(String message) {
        super(409, "BUSINESS_RULE_VIOLATION", message, null);
    }

    public BusinessException(String message, Throwable cause) {
        super(409, "BUSINESS_RULE_VIOLATION", message, cause);
    }
}
