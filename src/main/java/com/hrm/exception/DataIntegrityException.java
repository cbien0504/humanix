package com.hrm.exception;

public class DataIntegrityException extends AppException {

    private final String field;
    private final String constraint;

    public DataIntegrityException(String message, String field, String constraint, Throwable cause) {
        super(409, "DATA_INTEGRITY_VIOLATION", message, cause);
        this.field = field;
        this.constraint = constraint;
    }

    public String getField() {
        return field;
    }

    public String getConstraint() {
        return constraint;
    }
}
