package com.hrm.util;

import com.hrm.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class Validator {

    public static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    public static final Pattern PHONE = Pattern.compile("^(0|\\+84)\\d{9}$");
    public static final Pattern NATIONAL_ID = Pattern.compile("^\\d{12}$");
    public static final Pattern CODE = Pattern.compile("^[A-Z0-9-]{3,20}$");
    public static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9._-]{2,50}$");

    private final Map<String, String> errors = new LinkedHashMap<>();

    public Validator check(boolean condition, String field, String message) {
        if (!condition) {
            errors.putIfAbsent(field, message);
        }
        return this;
    }

    public Validator required(String field, Object value, String message) {
        boolean present = value != null && !(value instanceof String s && s.isBlank());
        return check(present, field, message);
    }

    public Validator maxLength(String field, String value, int max) {
        return check(value == null || value.length() <= max, field, "Tối đa " + max + " ký tự.");
    }

    public Validator pattern(String field, String value, Pattern pattern, String message) {
        return check(value == null || value.isBlank() || pattern.matcher(value).matches(), field, message);
    }

    public Validator email(String field, String value) {
        return pattern(field, value, EMAIL, "Email không hợp lệ.");
    }

    public Validator phone(String field, String value) {
        return pattern(field, value, PHONE, "Số điện thoại không hợp lệ (VD: 0912345678 hoặc +84912345678).");
    }

    public Validator nonNegative(String field, BigDecimal value, String message) {
        return check(value == null || value.signum() >= 0, field, message);
    }

    public Validator range(String field, Number value, long min, long max, String message) {
        return check(value == null || (value.longValue() >= min && value.longValue() <= max), field, message);
    }

    public Validator notBefore(String field, LocalDate end, LocalDate start, String message) {
        return check(end == null || start == null || !end.isBefore(start), field, message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasError(String field) {
        return errors.containsKey(field);
    }

    public Map<String, String> errors() {
        return errors;
    }

    public void throwIfInvalid() {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String upper(String value) {
        String v = clean(value);
        return v == null ? null : v.toUpperCase(Locale.ROOT);
    }

    public static String lower(String value) {
        String v = clean(value);
        return v == null ? null : v.toLowerCase(Locale.ROOT);
    }
}
