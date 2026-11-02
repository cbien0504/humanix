package com.hrm.model;

/** Review cycle status: DRAFT → OPEN → CLOSED (BR-50). */
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
