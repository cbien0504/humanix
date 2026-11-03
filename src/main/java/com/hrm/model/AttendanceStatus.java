package com.hrm.model;

public enum AttendanceStatus {
    PRESENT("Đúng giờ", 1.0),
    LATE("Đi muộn", 1.0),
    HALF_DAY("Nửa ngày", 0.5),
    ABSENT("Vắng", 0.0);

    private final String label;
    private final double workDay;

    AttendanceStatus(String label, double workDay) {
        this.label = label;
        this.workDay = workDay;
    }

    public String getLabel() {
        return label;
    }

    public double getWorkDay() {
        return workDay;
    }
}
