package com.hrm.security;

public enum Role {
    ADMIN("Quản trị hệ thống"),
    HR("Nhân sự"),
    MANAGER("Quản lý"),
    EMPLOYEE("Nhân viên");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
