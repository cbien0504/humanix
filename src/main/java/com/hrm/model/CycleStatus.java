package com.hrm.model;

public enum CycleStatus {
    DRAFT("Nháp"),
    OPEN("Đang mở"),
    CLOSED("Đã đóng");

    private final String label;

    CycleStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
