package com.hrm.model;

/** Lightweight employee entry for drop-down lists. */
public record EmployeeOption(long employeeId, String employeeCode, String fullName, long departmentId) {
}
