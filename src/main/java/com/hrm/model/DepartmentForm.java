package com.hrm.model;

public record DepartmentForm(String departmentCode, String departmentName, String description, Long managerId,
                             Boolean active) {
}
