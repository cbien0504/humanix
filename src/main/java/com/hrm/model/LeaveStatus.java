package com.hrm.model;

public enum LeaveStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Từ chối"),
    CANCELLED("Đã hủy");

    private final String label;

    LeaveStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
