package com.hrm.exception;

public class MethodNotAllowedException extends AppException {

    public MethodNotAllowedException(String method) {
        super(405, "METHOD_NOT_ALLOWED", "Phương thức " + method + " không được hỗ trợ cho đường dẫn này.", null);
    }
}
