package com.hrm.dao.jdbc;

import com.hrm.dao.AttendanceDao;
import com.hrm.model.AttendanceFilter;
import com.hrm.model.AttendanceRecord;
import com.hrm.model.AttendanceStatus;
import com.hrm.model.MonthlyAttendance;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcAttendanceDao implements AttendanceDao {

    private static final String SELECT = """
            SELECT a.AttendanceId, a.EmployeeId, e.EmployeeCode, e.FullName, d.DepartmentName, a.WorkDate,
                   a.CheckIn, a.CheckOut, a.Status, a.LateMinutes, a.WorkHours, a.Source, a.Note
            FROM dbo.Attendance AS a
            JOIN dbo.Employees AS e ON e.EmployeeId = a.EmployeeId
            JOIN dbo.Departments AS d ON d.DepartmentId = e.DepartmentId
            """;

    private static final Map<String, String> SORT = Map.of(
            "date", "a.WorkDate", "employee", "e.FullName", "code", "e.EmployeeCode", "status", "a.Status",
            "late", "a.LateMinutes", "hours", "a.WorkHours");

    private final JdbcExecutor jdbc;

    public JdbcAttendanceDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    static AttendanceRecord map(ResultSet rs) throws SQLException {
        AttendanceRecord r = new AttendanceRecord();
        r.setAttendanceId(rs.getLong("AttendanceId"));
        r.setEmployeeId(rs.getLong("EmployeeId"));
        r.setEmployeeCode(rs.getString("EmployeeCode"));
        r.setFullName(rs.getString("FullName"));
        r.setDepartmentName(rs.getString("DepartmentName"));
        r.setWorkDate(Rows.date(rs, "WorkDate"));
        r.setCheckIn(Rows.dateTime(rs, "CheckIn"));
        r.setCheckOut(Rows.dateTime(rs, "CheckOut"));
        r.setStatus(AttendanceStatus.valueOf(rs.getString("Status")));
        r.setLateMinutes(rs.getInt("LateMinutes"));
        r.setWorkHours(rs.getBigDecimal("WorkHours"));
        r.setSource(rs.getString("Source"));
        r.setNote(rs.getString("Note"));
        return r;
    }

    @Override
    public Optional<AttendanceRecord> find(long employeeId, LocalDate workDate) {
        return jdbc.queryOne(SELECT + " WHERE a.EmployeeId = ? AND a.WorkDate = ?", JdbcAttendanceDao::map, employeeId, workDate);
    }

    @Override
    public Optional<AttendanceRecord> findById(long attendanceId) {
        return jdbc.queryOne(SELECT + " WHERE a.AttendanceId = ?", JdbcAttendanceDao::map, attendanceId);
    }

    @Override
    public long insert(long employeeId, LocalDate workDate, LocalDateTime checkIn, LocalDateTime checkOut,
                       AttendanceStatus status, int lateMinutes, String source, String note) {
        return jdbc.insert("INSERT INTO dbo.Attendance (EmployeeId, WorkDate, CheckIn, CheckOut, Status, LateMinutes, Source, Note) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", employeeId, workDate, checkIn, checkOut, status, lateMinutes, source, note);
    }

    @Override
    public void update(long attendanceId, LocalDateTime checkIn, LocalDateTime checkOut, AttendanceStatus status,
                       int lateMinutes, String source, String note) {
        jdbc.update("UPDATE dbo.Attendance SET CheckIn = ?, CheckOut = ?, Status = ?, LateMinutes = ?, Source = ?, Note = ?, "
                + "UpdatedAt = SYSUTCDATETIME() WHERE AttendanceId = ?", checkIn, checkOut, status, lateMinutes, source, note, attendanceId);
    }

    @Override
    public void delete(long attendanceId) {
        jdbc.update("DELETE FROM dbo.Attendance WHERE AttendanceId = ?", attendanceId);
    }

    @Override
    public Page<AttendanceRecord> search(AttendanceFilter f, PageRequest page) {
        SqlWhere where = new SqlWhere()
                .andContains(f.keyword(), "e.EmployeeCode", "e.FullName")
                .andIfPresent("e.DepartmentId = ?", f.departmentId())
                .andIfPresent("a.EmployeeId = ?", f.employeeId())
                .andIfPresent("a.WorkDate >= ?", f.from())          // sargable range → IX_Attendance_WorkDate
                .andIfPresent("a.WorkDate <= ?", f.to())
                .andIfPresent("a.Status = ?", f.status() == null ? null : f.status().name());
        if (f.teamOfManagerId() != null) {
            where.and("(e.ManagerId = ? OR d.ManagerId = ? OR e.EmployeeId = ?)",
                    f.teamOfManagerId(), f.teamOfManagerId(), f.teamOfManagerId());
        }
        List<AttendanceRecord> items = jdbc.query(SELECT + where.toSql()
                        + SqlOrder.of(SORT, page, "a.WorkDate", "e.EmployeeCode") + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                JdbcAttendanceDao::map, where.paramsWith(page.offset(), page.size()));
        long total = jdbc.count("SELECT COUNT(*) FROM dbo.Attendance AS a JOIN dbo.Employees AS e ON e.EmployeeId = a.EmployeeId "
                + "JOIN dbo.Departments AS d ON d.DepartmentId = e.DepartmentId" + where.toSql(), where.params().toArray());
        return Page.of(items, page, total);
    }

    @Override
    public List<AttendanceRecord> findByEmployee(long employeeId, LocalDate from, LocalDate to) {
        return jdbc.query(SELECT + " WHERE a.EmployeeId = ? AND a.WorkDate >= ? AND a.WorkDate <= ? ORDER BY a.WorkDate DESC",
                JdbcAttendanceDao::map, employeeId, from, to);
    }

    @Override
    public Optional<MonthlyAttendance> monthly(long employeeId, int year, int month) {
        return jdbc.queryOne("""
                SELECT WorkYear, WorkMonth, PresentDays, LateDays, HalfDays, AbsentDays, WorkedDays, PaidLeaveDays,
                       UnpaidLeaveDays, TotalLateMinutes, TotalWorkHours
                FROM dbo.vw_MonthlyAttendanceSummary WHERE EmployeeId = ? AND WorkYear = ? AND WorkMonth = ?""",
                rs -> new MonthlyAttendance(rs.getInt("WorkYear"), rs.getInt("WorkMonth"), rs.getInt("PresentDays"),
                        rs.getInt("LateDays"), rs.getInt("HalfDays"), rs.getInt("AbsentDays"), rs.getBigDecimal("WorkedDays"),
                        rs.getInt("PaidLeaveDays"), rs.getInt("UnpaidLeaveDays"), rs.getInt("TotalLateMinutes"),
                        rs.getBigDecimal("TotalWorkHours")), employeeId, year, month);
    }

    @Override
    public long countCheckedIn(LocalDate workDate) {
        return jdbc.count("SELECT COUNT(*) FROM dbo.Attendance WHERE WorkDate = ? AND CheckIn IS NOT NULL", workDate);
    }
}
