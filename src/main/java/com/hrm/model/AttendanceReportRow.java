package com.hrm.model;

import java.math.BigDecimal;

/** Row of sp_GetAttendanceReport. */
public record AttendanceReportRow(long employeeId, String employeeCode, String fullName, long departmentId,
                                  String departmentName, String positionName, int expectedDays, int presentDays,
                                  int lateDays, int halfDays, int paidLeaveDays, int unpaidLeaveDays, int absentDays,
                                  BigDecimal workedDays, int lateMinutes, BigDecimal workHours, BigDecimal attendanceRate) {
}
