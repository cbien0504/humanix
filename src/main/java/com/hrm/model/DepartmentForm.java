package com.hrm.model;

/** Write model of a department. */
public record DepartmentForm(String departmentCode, String departmentName, String description, Long managerId,
                             Boolean active) {
}
