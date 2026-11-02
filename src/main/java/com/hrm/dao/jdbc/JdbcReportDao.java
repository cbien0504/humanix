package com.hrm.dao.jdbc;

import com.hrm.dao.ReportDao;
import com.hrm.model.AttendanceReportRow;
import com.hrm.model.DepartmentStat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JdbcReportDao implements ReportDao {

    private final JdbcExecutor jdbc;

    public JdbcReportDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<DepartmentStat> departmentStatistics(LocalDate asOf) {
        return jdbc.call("{call dbo.sp_GetDepartmentStatistics(?)}", rs -> new DepartmentStat(rs.getLong("DepartmentId"),
                rs.getString("DepartmentCode"), rs.getString("DepartmentName"), rs.getString("ManagerName"),
                rs.getInt("Headcount"), rs.getInt("ActiveCount"), rs.getInt("ProbationCount"), rs.getInt("OnLeaveCount"),
                rs.getInt("MaleCount"), rs.getInt("FemaleCount"), rs.getBigDecimal("AvgBaseSalary"),
                rs.getBigDecimal("MonthlySalaryFund"), rs.getBigDecimal("AvgYearsOfService"), rs.getBigDecimal("AvgAge"),
                rs.getInt("NewHiresYtd"), rs.getInt("TerminationsYtd"), rs.getBigDecimal("TurnoverRate")), asOf);
    }

    @Override
    public List<AttendanceReportRow> attendanceReport(LocalDate from, LocalDate to, Long departmentId) {
        return jdbc.call("{call dbo.sp_GetAttendanceReport(?, ?, ?)}", rs -> new AttendanceReportRow(rs.getLong("EmployeeId"),
                rs.getString("EmployeeCode"), rs.getString("FullName"), rs.getLong("DepartmentId"), rs.getString("DepartmentName"),
                rs.getString("PositionName"), rs.getInt("ExpectedDays"), rs.getInt("PresentDays"), rs.getInt("LateDays"),
                rs.getInt("HalfDays"), rs.getInt("PaidLeaveDays"), rs.getInt("UnpaidLeaveDays"), rs.getInt("AbsentDays"),
                rs.getBigDecimal("WorkedDays"), rs.getInt("LateMinutes"), rs.getBigDecimal("WorkHours"),
                rs.getBigDecimal("AttendanceRate")), from, to, departmentId);
    }

    @Override
    public Map<String, Integer> headcountSummary(LocalDate today) {
        return jdbc.queryOne("""
                SELECT SUM(CASE WHEN Status <> 'TERMINATED' THEN 1 ELSE 0 END) AS headcount,
                       SUM(CASE WHEN Status = 'ACTIVE' THEN 1 ELSE 0 END) AS active,
                       SUM(CASE WHEN Status = 'PROBATION' THEN 1 ELSE 0 END) AS probation,
                       SUM(CASE WHEN Status = 'ON_LEAVE' THEN 1 ELSE 0 END) AS onLongLeave,
                       SUM(CASE WHEN Status <> 'TERMINATED' AND Gender = 'M' THEN 1 ELSE 0 END) AS male,
                       SUM(CASE WHEN Status <> 'TERMINATED' AND Gender = 'F' THEN 1 ELSE 0 END) AS female,
                       SUM(CASE WHEN Status <> 'TERMINATED' AND Gender = 'O' THEN 1 ELSE 0 END) AS other,
                       SUM(CASE WHEN HireDate >= DATEFROMPARTS(YEAR(?), MONTH(?), 1) AND HireDate <= ? THEN 1 ELSE 0 END) AS newHiresMonth,
                       SUM(CASE WHEN TerminationDate >= DATEFROMPARTS(YEAR(?), 1, 1) AND TerminationDate <= ? THEN 1 ELSE 0 END) AS leaversYtd
                FROM dbo.Employees""", JdbcReportDao::row, today, today, today, today, today).orElse(Map.of());
    }

    @Override
    public Map<String, Integer> attendanceOn(LocalDate day) {
        return jdbc.queryOne("""
                SELECT (SELECT COUNT(*) FROM dbo.Attendance WHERE WorkDate = ? AND CheckIn IS NOT NULL) AS checkedIn,
                       (SELECT COUNT(*) FROM dbo.Attendance WHERE WorkDate = ? AND Status = 'LATE') AS late,
                       (SELECT COUNT(DISTINCT EmployeeId) FROM dbo.LeaveRequests
                        WHERE Status = 'APPROVED' AND StartDate <= ? AND EndDate >= ?) AS onLeave,
                       (SELECT COUNT(*) FROM dbo.Employees WHERE Status IN ('ACTIVE', 'PROBATION') AND HireDate <= ?) AS expected""",
                JdbcReportDao::row, day, day, day, day, day).orElse(Map.of());
    }

    @Override
    public List<Map<String, Object>> attendanceTrend(LocalDate from, LocalDate to) {
        return jdbc.query("""
                SELECT w.WorkDate,
                       SUM(CASE WHEN a.Status = 'PRESENT' THEN 1 ELSE 0 END) AS onTime,
                       SUM(CASE WHEN a.Status = 'LATE' THEN 1 ELSE 0 END) AS late,
                       SUM(CASE WHEN a.Status = 'HALF_DAY' THEN 1 ELSE 0 END) AS halfDay
                FROM dbo.fn_WorkingDays(?, ?) AS w
                LEFT JOIN dbo.Attendance AS a ON a.WorkDate = w.WorkDate
                GROUP BY w.WorkDate ORDER BY w.WorkDate""", rs -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", Rows.date(rs, "WorkDate"));
            m.put("onTime", rs.getInt("onTime"));
            m.put("late", rs.getInt("late"));
            m.put("halfDay", rs.getInt("halfDay"));
            return m;
        }, from, to);
    }

    @Override
    public List<Map<String, Object>> payrollTrend(int count) {
        return jdbc.query("""
                SELECT PeriodYear, PeriodMonth, TotalNet, TotalGross, EmployeeCount, Status FROM (
                    SELECT TOP (?) PeriodYear, PeriodMonth, TotalNet, TotalGross, EmployeeCount, Status
                    FROM dbo.PayrollPeriods WHERE Status <> 'OPEN' ORDER BY PeriodYear DESC, PeriodMonth DESC) AS t
                ORDER BY PeriodYear, PeriodMonth""", rs -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("year", rs.getInt("PeriodYear"));
            m.put("month", rs.getInt("PeriodMonth"));
            m.put("totalNet", rs.getBigDecimal("TotalNet"));
            m.put("totalGross", rs.getBigDecimal("TotalGross"));
            m.put("employees", rs.getInt("EmployeeCount"));
            m.put("status", rs.getString("Status"));
            return m;
        }, count);
    }

    @Override
    public Map<String, Integer> teamSummary(long managerId, LocalDate today) {
        // LEFT JOIN instead of SUM(CASE WHEN EXISTS ...) – SQL Server error 130 forbids aggregating a subquery;
        // UQ_Attendance_EmployeeDate guarantees at most one joined row per employee
        return jdbc.queryOne("""
                SELECT COUNT(*) AS teamSize, COUNT(a.AttendanceId) AS checkedIn
                FROM dbo.Employees AS e
                JOIN dbo.Departments AS d ON d.DepartmentId = e.DepartmentId
                LEFT JOIN dbo.Attendance AS a ON a.EmployeeId = e.EmployeeId AND a.WorkDate = ? AND a.CheckIn IS NOT NULL
                WHERE e.Status IN ('ACTIVE', 'PROBATION') AND e.EmployeeId <> ? AND (e.ManagerId = ? OR d.ManagerId = ?)""",
                JdbcReportDao::row, today, managerId, managerId, managerId).orElse(Map.of());
    }

    /** Maps every column of the row to an int (NULL → 0). */
    private static Map<String, Integer> row(ResultSet rs) throws SQLException {
        Map<String, Integer> m = new LinkedHashMap<>();
        int n = rs.getMetaData().getColumnCount();
        for (int i = 1; i <= n; i++) {
            m.put(rs.getMetaData().getColumnLabel(i), rs.getInt(i));
        }
        return m;
    }
}
