package com.hrm.model;

public enum ReviewStatus {
    PENDING_SELF("Chờ tự đánh giá"),
    PENDING_MANAGER("Chờ quản lý chấm"),
    COMPLETED("Hoàn thành");

    private final String label;

    ReviewStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
