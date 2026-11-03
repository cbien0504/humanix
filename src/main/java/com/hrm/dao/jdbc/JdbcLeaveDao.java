package com.hrm.dao.jdbc;

import com.hrm.dao.LeaveDao;
import com.hrm.model.LeaveBalance;
import com.hrm.model.LeaveFilter;
import com.hrm.model.LeaveRequest;
import com.hrm.model.LeaveRequestForm;
import com.hrm.model.LeaveStatus;
import com.hrm.model.LeaveType;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcLeaveDao implements LeaveDao {

    private static final String SELECT = """
            SELECT lr.LeaveRequestId, lr.EmployeeId, e.EmployeeCode, e.FullName, d.DepartmentName, lr.LeaveTypeId,
                   lt.LeaveTypeCode, lt.LeaveTypeName, lt.IsPaid, lr.StartDate, lr.EndDate, lr.TotalDays, lr.Reason,
                   lr.Status, lr.ApproverId, a.FullName AS ApproverName, lr.DecidedAt, lr.ApproverComment, lr.CreatedAt
            FROM dbo.LeaveRequests AS lr
            JOIN dbo.Employees AS e ON e.EmployeeId = lr.EmployeeId
            JOIN dbo.Departments AS d ON d.DepartmentId = e.DepartmentId
            JOIN dbo.LeaveTypes AS lt ON lt.LeaveTypeId = lr.LeaveTypeId
            LEFT JOIN dbo.Employees AS a ON a.EmployeeId = lr.ApproverId
            """;

    private static final Map<String, String> SORT = Map.of(
            "startDate", "lr.StartDate", "createdAt", "lr.CreatedAt", "employee", "e.FullName", "status", "lr.Status",
            "days", "lr.TotalDays");

    private final JdbcExecutor jdbc;

    public JdbcLeaveDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    static LeaveRequest map(ResultSet rs) throws SQLException {
        LeaveRequest r = new LeaveRequest();
        r.setLeaveRequestId(rs.getLong("LeaveRequestId"));
        r.setEmployeeId(rs.getLong("EmployeeId"));
        r.setEmployeeCode(rs.getString("EmployeeCode"));
        r.setFullName(rs.getString("FullName"));
        r.setDepartmentName(rs.getString("DepartmentName"));
        r.setLeaveTypeId(rs.getLong("LeaveTypeId"));
        r.setLeaveTypeCode(rs.getString("LeaveTypeCode"));
        r.setLeaveTypeName(rs.getString("LeaveTypeName"));
        r.setPaid(rs.getBoolean("IsPaid"));
        r.setStartDate(Rows.date(rs, "StartDate"));
        r.setEndDate(Rows.date(rs, "EndDate"));
        r.setTotalDays(rs.getBigDecimal("TotalDays"));
        r.setReason(rs.getString("Reason"));
        r.setStatus(LeaveStatus.valueOf(rs.getString("Status")));
        r.setApproverId(Rows.longValue(rs, "ApproverId"));
        r.setApproverName(rs.getString("ApproverName"));
        r.setDecidedAt(Rows.instant(rs, "DecidedAt"));
        r.setApproverComment(rs.getString("ApproverComment"));
        r.setCreatedAt(Rows.instant(rs, "CreatedAt"));
        return r;
    }

    private static LeaveType mapType(ResultSet rs) throws SQLException {
        return new LeaveType(rs.getLong("LeaveTypeId"), rs.getString("LeaveTypeCode"), rs.getString("LeaveTypeName"),
                rs.getBoolean("IsPaid"), rs.getBoolean("RequiresBalance"), rs.getBigDecimal("MaxDaysPerRequest"),
                rs.getBoolean("IsActive"));
    }

    @Override
    public List<LeaveType> leaveTypes() {
        return jdbc.query("SELECT * FROM dbo.LeaveTypes WHERE IsActive = 1 ORDER BY LeaveTypeId", JdbcLeaveDao::mapType);
    }

    @Override
    public Optional<LeaveType> leaveType(long leaveTypeId) {
        return jdbc.queryOne("SELECT * FROM dbo.LeaveTypes WHERE LeaveTypeId = ?", JdbcLeaveDao::mapType, leaveTypeId);
    }

    @Override
    public Optional<LeaveRequest> findById(long leaveRequestId) {
        return jdbc.queryOne(SELECT + " WHERE lr.LeaveRequestId = ?", JdbcLeaveDao::map, leaveRequestId);
    }

    @Override
    public List<LeaveRequest> findOverlapping(long employeeId, LocalDate start, LocalDate end) {
        return jdbc.query(SELECT + " WHERE lr.EmployeeId = ? AND lr.StartDate <= ? AND lr.EndDate >= ? "
                + "AND lr.Status IN ('PENDING', 'APPROVED') ORDER BY lr.StartDate", JdbcLeaveDao::map, employeeId, end, start);
    }

    @Override
    public long insert(long employeeId, LeaveRequestForm f, BigDecimal totalDays) {
        return jdbc.insert("INSERT INTO dbo.LeaveRequests (EmployeeId, LeaveTypeId, StartDate, EndDate, TotalDays, Reason) "
                + "VALUES (?, ?, ?, ?, ?, ?)", employeeId, f.leaveTypeId(), f.startDate(), f.endDate(), totalDays, f.reason());
    }

    @Override
    public int changeStatus(long leaveRequestId, LeaveStatus expected, LeaveStatus target, Long approverId, String comment) {
        if (target == LeaveStatus.CANCELLED) {
            return jdbc.update("UPDATE dbo.LeaveRequests SET Status = ?, UpdatedAt = SYSUTCDATETIME() "
                    + "WHERE LeaveRequestId = ? AND Status = ?", target, leaveRequestId, expected);
        }
        return jdbc.update("UPDATE dbo.LeaveRequests SET Status = ?, ApproverId = ?, ApproverComment = ?, "
                        + "DecidedAt = SYSUTCDATETIME(), UpdatedAt = SYSUTCDATETIME() WHERE LeaveRequestId = ? AND Status = ?",
                target, approverId, comment, leaveRequestId, expected);
    }

    @Override
    public Page<LeaveRequest> search(LeaveFilter f, PageRequest page) {
        SqlWhere where = new SqlWhere()
                .andContains(f.keyword(), "e.EmployeeCode", "e.FullName")
                .andIf(f.status() == LeaveStatus.PENDING, "lr.Status = 'PENDING'")
                .andIf(f.status() != null && f.status() != LeaveStatus.PENDING, "lr.Status = ?",
                        f.status() == null ? null : f.status().name())
                .andIfPresent("lr.LeaveTypeId = ?", f.leaveTypeId())
                .andIfPresent("lr.EmployeeId = ?", f.employeeId());
        if (f.year() != null) {
            where.and("lr.StartDate >= ? AND lr.StartDate < ?", LocalDate.of(f.year(), 1, 1), LocalDate.of(f.year() + 1, 1, 1));
        }
        if (f.teamOfManagerId() != null) {
            where.and("e.EmployeeId <> ? AND (e.ManagerId = ? OR d.ManagerId = ?)",
                    f.teamOfManagerId(), f.teamOfManagerId(), f.teamOfManagerId());
        }
        List<LeaveRequest> items = jdbc.query(SELECT + where.toSql()
                        + SqlOrder.of(SORT, page, "lr.StartDate", "lr.LeaveRequestId") + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                JdbcLeaveDao::map, where.paramsWith(page.offset(), page.size()));
        long total = jdbc.count("SELECT COUNT(*) FROM dbo.LeaveRequests AS lr JOIN dbo.Employees AS e ON e.EmployeeId = lr.EmployeeId "
                + "JOIN dbo.Departments AS d ON d.DepartmentId = e.DepartmentId" + where.toSql(), where.params().toArray());
        return Page.of(items, page, total);
    }

    @Override
    public List<LeaveBalance> balances(long employeeId, int year) {
        return jdbc.query("""
                SELECT lt.LeaveTypeId, lt.LeaveTypeCode, lt.LeaveTypeName, lb.BalanceYear, lb.EntitledDays, lb.UsedDays,
                       ISNULL((SELECT SUM(lr.TotalDays) FROM dbo.LeaveRequests AS lr
                               WHERE lr.EmployeeId = lb.EmployeeId AND lr.LeaveTypeId = lb.LeaveTypeId AND lr.Status = 'PENDING'
                                 AND lr.StartDate >= DATEFROMPARTS(lb.BalanceYear, 1, 1)
                                 AND lr.StartDate < DATEFROMPARTS(lb.BalanceYear + 1, 1, 1)), 0) AS PendingDays
                FROM dbo.LeaveBalances AS lb
                JOIN dbo.LeaveTypes AS lt ON lt.LeaveTypeId = lb.LeaveTypeId
                WHERE lb.EmployeeId = ? AND lb.BalanceYear = ?""",
                rs -> new LeaveBalance(rs.getLong("LeaveTypeId"), rs.getString("LeaveTypeCode"), rs.getString("LeaveTypeName"),
                        rs.getInt("BalanceYear"), rs.getBigDecimal("EntitledDays"), rs.getBigDecimal("UsedDays"),
                        rs.getBigDecimal("PendingDays")), employeeId, year);
    }

    @Override
    public boolean hasApprovedLeaveOn(long employeeId, LocalDate date) {
        return jdbc.count("SELECT COUNT(*) FROM dbo.LeaveRequests WHERE EmployeeId = ? AND Status = 'APPROVED' "
                + "AND StartDate <= ? AND EndDate >= ?", employeeId, date, date) > 0;
    }

    @Override
    public int initBalances(int year) {
        return jdbc.call("{call dbo.sp_InitLeaveBalances(?)}", rs -> rs.getInt("InsertedCount") + rs.getInt("UpdatedCount"), year)
                .stream().findFirst().orElse(0);
    }

    @Override
    public long countPending(Long teamOfManagerId) {
        if (teamOfManagerId == null) {
            return jdbc.count("SELECT COUNT(*) FROM dbo.LeaveRequests WHERE Status = 'PENDING'");
        }
        return jdbc.count("""
                SELECT COUNT(*) FROM dbo.LeaveRequests AS lr JOIN dbo.Employees AS e ON e.EmployeeId = lr.EmployeeId
                JOIN dbo.Departments AS d ON d.DepartmentId = e.DepartmentId
                WHERE lr.Status = 'PENDING' AND e.EmployeeId <> ? AND (e.ManagerId = ? OR d.ManagerId = ?)""",
                teamOfManagerId, teamOfManagerId, teamOfManagerId);
    }
}
