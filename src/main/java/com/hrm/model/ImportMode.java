package com.hrm.model;

import java.util.Locale;

public enum ImportMode {
    VALIDATE_ONLY,
    VALID_ONLY,
    ALL_OR_NOTHING;

    public static ImportMode parse(String value) {
        if (value == null || value.isBlank()) {
            return VALIDATE_ONLY;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT).replace('_', '-')) {
            case "validate", "validate-only", "dry-run" -> VALIDATE_ONLY;
            case "valid-only", "skip-invalid" -> VALID_ONLY;
            case "all-or-nothing", "atomic" -> ALL_OR_NOTHING;
            default -> throw new IllegalArgumentException("Chế độ import không hợp lệ: " + value
                    + " (validate | valid-only | all-or-nothing)");
        };
    }
}
