package com.hrm.model;

import java.util.List;

/** Outcome of an import: counters plus every row error. */
public record ImportResult(String fileName, ImportMode mode, int totalRows, int validRows, int importedRows,
                          List<ImportRowError> errors) {

    public int invalidRows() {
        return (int) errors.stream().mapToInt(ImportRowError::row).distinct().count();
    }

    public boolean success() {
        return errors.isEmpty();
    }
}
