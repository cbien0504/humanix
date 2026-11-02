package com.hrm.model;

/** Employment status (CK_Employees_Status). */
public enum EmployeeStatus {
    PROBATION("Thử việc"),
    ACTIVE("Chính thức"),
    ON_LEAVE("Nghỉ dài hạn"),
    TERMINATED("Đã nghỉ việc");

    private final String label;

    EmployeeStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Still employed (eligible for attendance, reviews…). */
    public boolean isWorking() {
        return this == PROBATION || this == ACTIVE;
    }
}
