package com.hrm.model;

/** One problem found in a data row (row numbers are 1-based and include the header row). */
public record ImportRowError(int row, String column, String value, String message) {
}
