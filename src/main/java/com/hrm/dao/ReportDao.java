package com.hrm.dao;

import com.hrm.model.AttendanceReportRow;
import com.hrm.model.DepartmentStat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReportDao {

    List<DepartmentStat> departmentStatistics(LocalDate asOf);

    List<AttendanceReportRow> attendanceReport(LocalDate from, LocalDate to, Long departmentId);

    Map<String, Integer> headcountSummary(LocalDate today);

    Map<String, Integer> attendanceOn(LocalDate day);

    List<Map<String, Object>> attendanceTrend(LocalDate from, LocalDate to);

    List<Map<String, Object>> payrollTrend(int count);

    Map<String, Integer> teamSummary(long managerEmployeeId, LocalDate today);
}
