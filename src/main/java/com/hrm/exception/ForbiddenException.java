package com.hrm.exception;

public class ForbiddenException extends AppException {

    public ForbiddenException(String message) {
        super(403, "FORBIDDEN", message, null);
    }

    public ForbiddenException() {
        this("Bạn không có quyền thực hiện thao tác này.");
    }
}
