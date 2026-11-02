package com.hrm.model;

import java.math.BigDecimal;

/** Row of vw_MonthlyAttendanceSummary for one employee and month. */
public record MonthlyAttendance(int year, int month, int presentDays, int lateDays, int halfDays, int absentDays,
                                BigDecimal workedDays, int paidLeaveDays, int unpaidLeaveDays, int totalLateMinutes,
                                BigDecimal totalWorkHours) {

    public static MonthlyAttendance empty(int year, int month) {
        return new MonthlyAttendance(year, month, 0, 0, 0, 0, BigDecimal.ZERO, 0, 0, 0, BigDecimal.ZERO);
    }
}
