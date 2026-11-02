package com.hrm.exception;

/** The caller is authenticated but not allowed to perform the operation (HTTP 403). */
public class ForbiddenException extends AppException {

    public ForbiddenException(String message) {
        super(403, "FORBIDDEN", message, null);
    }

    public ForbiddenException() {
        this("Bạn không có quyền thực hiện thao tác này.");
    }
}
