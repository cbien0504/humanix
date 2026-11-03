package com.hrm.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ValidationException extends AppException {

    private final Map<String, String> fieldErrors;

    public ValidationException(Map<String, String> fieldErrors) {
        super(400, "VALIDATION_FAILED", "Dữ liệu không hợp lệ, vui lòng kiểm tra lại.", null);
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public ValidationException(String field, String message) {
        this(Map.of(field, message));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    @Override
    public String getMessage() {
        return fieldErrors.size() == 1 ? fieldErrors.values().iterator().next() : super.getMessage();
    }
}
