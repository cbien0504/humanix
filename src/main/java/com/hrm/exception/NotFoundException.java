package com.hrm.exception;

/** Requested resource does not exist or is not visible to the current user (HTTP 404). */
public class NotFoundException extends AppException {

    public NotFoundException(String message) {
        super(404, "NOT_FOUND", message, null);
    }

    public static NotFoundException of(String entity, Object id) {
        return new NotFoundException("Không tìm thấy " + entity + " (" + id + ").");
    }
}
