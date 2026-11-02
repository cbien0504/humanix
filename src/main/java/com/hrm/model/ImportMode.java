package com.hrm.model;

import java.util.Locale;

/** How an import file is applied. */
public enum ImportMode {
    /** Validate every row, write nothing (dry run). */
    VALIDATE_ONLY,
    /** Import valid rows, report the invalid ones. */
    VALID_ONLY,
    /** Import only if every row is valid – one transaction. */
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
