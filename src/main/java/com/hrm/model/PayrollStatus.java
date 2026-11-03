package com.hrm.model;

public enum PayrollStatus {
    OPEN("Mở"),
    CALCULATED("Đã tính"),
    APPROVED("Đã duyệt"),
    PAID("Đã chi trả");

    private final String label;

    PayrollStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isRecalculable() {
        return this == OPEN || this == CALCULATED;
    }
}
