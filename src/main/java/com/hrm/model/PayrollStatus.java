package com.hrm.model;

/** Payroll period status (CK_PayrollPeriods_Status): OPEN → CALCULATED → APPROVED → PAID. */
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

    /** BR-30: approved and paid periods are frozen. */
    public boolean isRecalculable() {
        return this == OPEN || this == CALCULATED;
    }
}
