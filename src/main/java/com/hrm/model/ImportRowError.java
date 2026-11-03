package com.hrm.model;

public record ImportRowError(int row, String column, String value, String message) {
}
