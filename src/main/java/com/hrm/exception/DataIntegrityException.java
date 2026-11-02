package com.hrm.exception;

/**
 * The database rejected a write because of a UNIQUE / FOREIGN KEY / CHECK constraint (HTTP 409).
 * The message is user-friendly; {@link #getField()} names the input field when it is known so
 * the UI can highlight it (e.g. a duplicate employee code).
 */
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
