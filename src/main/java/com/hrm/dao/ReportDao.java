package com.hrm.dao;

import com.hrm.model.AttendanceReportRow;
import com.hrm.model.DepartmentStat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Reporting and dashboard queries (read-only). */
public interface ReportDao {

    List<DepartmentStat> departmentStatistics(LocalDate asOf);

    List<AttendanceReportRow> attendanceReport(LocalDate from, LocalDate to, Long departmentId);

    /** Company headcount figures as of {@code today}: status, gender, new hires (month), leavers (year). */
    Map<String, Integer> headcountSummary(LocalDate today);

    /** Attendance of {@code day}: checked in, late, on approved leave, expected (working) employees. */
    Map<String, Integer> attendanceOn(LocalDate day);

    /** Daily present / late / half-day counts over the working days of [from, to]. */
    List<Map<String, Object>> attendanceTrend(LocalDate from, LocalDate to);

    /** Last {@code count} calculated payroll periods (oldest first): year, month, totalNet, employees. */
    List<Map<String, Object>> payrollTrend(int count);

    /** Team figures of a manager: size and checked in today. */
    Map<String, Integer> teamSummary(long managerEmployeeId, LocalDate today);
}
