package com.hrm.dao.jdbc;

import com.hrm.dao.EmployeeDao;
import com.hrm.model.Employee;
import com.hrm.model.EmployeeFilter;
import com.hrm.model.EmployeeForm;
import com.hrm.model.EmployeeOption;
import com.hrm.model.EmployeeStatus;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.model.SalaryChange;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** JDBC implementation of {@link EmployeeDao}; reads go through the view vw_EmployeeDetails. */
public class JdbcEmployeeDao implements EmployeeDao {

    static final String SELECT = """
            SELECT e.EmployeeId, e.EmployeeCode, e.FullName, e.Gender, e.DateOfBirth, e.Age, e.Email, e.Phone, e.Address,
                   e.NationalId, e.TaxCode, e.BankAccount, e.BankName, e.DepartmentId, e.DepartmentCode, e.DepartmentName,
                   e.PositionId, e.PositionCode, e.PositionName, e.ManagerId, e.ManagerCode, e.ManagerName,
                   e.HireDate, e.TerminationDate, e.YearsOfService, e.Status, e.BaseSalary, e.Allowance, e.Dependents,
                   e.Username, e.HasActiveAccount, e.CreatedAt, e.UpdatedAt
            FROM dbo.vw_EmployeeDetails AS e
            """;

    /** Whitelist: API sort key → column (ORDER BY cannot be a bind parameter). */
    static final Map<String, String> SORT_COLUMNS = Map.of(
            "code", "e.EmployeeCode", "name", "e.FullName", "department", "e.DepartmentName",
            "position", "e.PositionName", "hireDate", "e.HireDate", "status", "e.Status", "salary", "e.BaseSalary");

    private final JdbcExecutor jdbc;

    public JdbcEmployeeDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    static Employee map(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setEmployeeId(rs.getLong("EmployeeId"));
        e.setEmployeeCode(rs.getString("EmployeeCode"));
        e.setFullName(rs.getString("FullName"));
        e.setGender(rs.getString("Gender"));
        e.setDateOfBirth(Rows.date(rs, "DateOfBirth"));
        e.setAge(Rows.integer(rs, "Age"));
        e.setEmail(rs.getString("Email"));
        e.setPhone(rs.getString("Phone"));
        e.setAddress(rs.getString("Address"));
        e.setNationalId(rs.getString("NationalId"));
        e.setTaxCode(rs.getString("TaxCode"));
        e.setBankAccount(rs.getString("BankAccount"));
        e.setBankName(rs.getString("BankName"));
        e.setDepartmentId(Rows.longValue(rs, "DepartmentId"));
        e.setDepartmentCode(rs.getString("DepartmentCode"));
        e.setDepartmentName(rs.getString("DepartmentName"));
        e.setPositionId(Rows.longValue(rs, "PositionId"));
        e.setPositionCode(rs.getString("PositionCode"));
        e.setPositionName(rs.getString("PositionName"));
        e.setManagerId(Rows.longValue(rs, "ManagerId"));
        e.setManagerCode(rs.getString("ManagerCode"));
        e.setManagerName(rs.getString("ManagerName"));
        e.setHireDate(Rows.date(rs, "HireDate"));
        e.setTerminationDate(Rows.date(rs, "TerminationDate"));
        e.setYearsOfService(rs.getBigDecimal("YearsOfService"));
        e.setStatus(EmployeeStatus.valueOf(rs.getString("Status")));
        e.setBaseSalary(rs.getBigDecimal("BaseSalary"));
        e.setAllowance(rs.getBigDecimal("Allowance"));
        e.setDependents(Rows.integer(rs, "Dependents"));
        e.setUsername(rs.getString("Username"));
        e.setHasActiveAccount(rs.getBoolean("HasActiveAccount"));
        e.setCreatedAt(Rows.instant(rs, "CreatedAt"));
        e.setUpdatedAt(Rows.instant(rs, "UpdatedAt"));
        return e;
    }

    static SqlWhere where(EmployeeFilter f) {
        SqlWhere w = new SqlWhere()
                .andContains(f.keyword(), "e.EmployeeCode", "e.FullName", "e.Email", "e.Phone")
                .andIfPresent("e.DepartmentId = ?", f.departmentId())
                .andIfPresent("e.PositionId = ?", f.positionId())
                .andIfPresent("e.Status = ?", f.status() == null ? null : f.status().name());
        if (f.teamOfManagerId() != null) {
            w.and("e.EmployeeId <> ? AND (e.ManagerId = ? OR e.DepartmentId IN "
                            + "(SELECT d.DepartmentId FROM dbo.Departments AS d WHERE d.ManagerId = ?))",
                    f.teamOfManagerId(), f.teamOfManagerId(), f.teamOfManagerId());
        }
        return w;
    }

    @Override
    public Page<Employee> search(EmployeeFilter filter, PageRequest page) {
        SqlWhere where = where(filter);
        List<Employee> items = jdbc.query(SELECT + where.toSql() + SqlOrder.of(SORT_COLUMNS, page, "e.EmployeeCode", "e.EmployeeId")
                + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", JdbcEmployeeDao::map, where.paramsWith(page.offset(), page.size()));
        long total = jdbc.count("SELECT COUNT(*) FROM dbo.vw_EmployeeDetails AS e" + where.toSql(), where.params().toArray());
        return Page.of(items, page, total);
    }

    @Override
    public List<Employee> findAll(EmployeeFilter filter) {
        SqlWhere where = where(filter);
        return jdbc.query(SELECT + where.toSql() + " ORDER BY e.EmployeeCode", JdbcEmployeeDao::map, where.params().toArray());
    }

    @Override
    public Optional<Employee> findById(long id) {
        return jdbc.queryOne(SELECT + " WHERE e.EmployeeId = ?", JdbcEmployeeDao::map, id);
    }

    @Override
    public Optional<Employee> findByCode(String code) {
        return jdbc.queryOne(SELECT + " WHERE e.EmployeeCode = ?", JdbcEmployeeDao::map, code);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jdbc.count("SELECT COUNT(*) FROM dbo.Employees WHERE Email = ?", email) > 0;
    }

    @Override
    public long insert(EmployeeForm f) {
        return jdbc.insert("""
                INSERT INTO dbo.Employees (EmployeeCode, FullName, Gender, DateOfBirth, Email, Phone, Address, NationalId, TaxCode,
                    BankAccount, BankName, DepartmentId, PositionId, ManagerId, HireDate, TerminationDate, Status,
                    BaseSalary, Allowance, Dependents)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", params(f));
    }

    @Override
    public void update(long id, EmployeeForm f) {
        Object[] values = params(f);
        Object[] all = java.util.Arrays.copyOf(values, values.length + 1);
        all[values.length] = id;
        jdbc.update("""
                UPDATE dbo.Employees SET EmployeeCode = ?, FullName = ?, Gender = ?, DateOfBirth = ?, Email = ?, Phone = ?,
                    Address = ?, NationalId = ?, TaxCode = ?, BankAccount = ?, BankName = ?, DepartmentId = ?, PositionId = ?,
                    ManagerId = ?, HireDate = ?, TerminationDate = ?, Status = ?, BaseSalary = ?, Allowance = ?, Dependents = ?,
                    UpdatedAt = SYSUTCDATETIME()
                WHERE EmployeeId = ?""", all);
    }

    private static Object[] params(EmployeeForm f) {
        return new Object[]{f.employeeCode(), f.fullName(), f.gender(), f.dateOfBirth(), f.email(), f.phone(), f.address(),
                f.nationalId(), f.taxCode(), f.bankAccount(), f.bankName(), f.departmentId(), f.positionId(), f.managerId(),
                f.hireDate(), f.terminationDate(), f.status(), f.baseSalary(), f.allowance(), f.dependents()};
    }

    @Override
    public void delete(long id) {
        jdbc.update("DELETE FROM dbo.LeaveBalances WHERE EmployeeId = ? AND UsedDays = 0", id);
        jdbc.update("DELETE FROM dbo.Employees WHERE EmployeeId = ?", id);
    }

    @Override
    public List<String> dependencies(long id) {
        return jdbc.query("""
                SELECT N'chấm công' WHERE EXISTS (SELECT 1 FROM dbo.Attendance WHERE EmployeeId = ?)
                UNION ALL SELECT N'đơn nghỉ phép' WHERE EXISTS (SELECT 1 FROM dbo.LeaveRequests WHERE EmployeeId = ? OR ApproverId = ?)
                UNION ALL SELECT N'bảng lương' WHERE EXISTS (SELECT 1 FROM dbo.Payroll WHERE EmployeeId = ?)
                UNION ALL SELECT N'đánh giá hiệu suất' WHERE EXISTS (SELECT 1 FROM dbo.PerformanceReviews WHERE EmployeeId = ? OR ReviewerId = ?)
                UNION ALL SELECT N'quản lý nhân viên khác' WHERE EXISTS (SELECT 1 FROM dbo.Employees WHERE ManagerId = ?)
                UNION ALL SELECT N'trưởng phòng' WHERE EXISTS (SELECT 1 FROM dbo.Departments WHERE ManagerId = ?)
                UNION ALL SELECT N'hồ sơ tuyển dụng' WHERE EXISTS (SELECT 1 FROM dbo.Candidates WHERE HiredEmployeeId = ?)
                UNION ALL SELECT N'ngày phép đã dùng' WHERE EXISTS (SELECT 1 FROM dbo.LeaveBalances WHERE EmployeeId = ? AND UsedDays > 0)
                """, rs -> rs.getString(1), id, id, id, id, id, id, id, id, id, id);
    }

    @Override
    public List<SalaryChange> salaryHistory(long id) {
        return jdbc.query("SELECT OldBaseSalary, NewBaseSalary, OldAllowance, NewAllowance, ChangedBy, ChangedAt "
                        + "FROM dbo.SalaryHistory WHERE EmployeeId = ? ORDER BY ChangedAt DESC, SalaryHistoryId DESC",
                rs -> new SalaryChange(rs.getBigDecimal("OldBaseSalary"), rs.getBigDecimal("NewBaseSalary"),
                        rs.getBigDecimal("OldAllowance"), rs.getBigDecimal("NewAllowance"), rs.getString("ChangedBy"),
                        Rows.instant(rs, "ChangedAt")), id);
    }

    @Override
    public String nextCode(String prefix) {
        Integer max = jdbc.queryScalar("SELECT MAX(TRY_CAST(SUBSTRING(EmployeeCode, ? + 1, 20) AS INT)) FROM dbo.Employees "
                + "WHERE EmployeeCode LIKE ?", Integer.class, prefix.length(), SqlWhere.escapeLike(prefix) + "[0-9]%");
        return prefix + String.format("%04d", (max == null ? 0 : max) + 1);
    }

    @Override
    public boolean isInTeam(long managerEmployeeId, long employeeId) {
        return jdbc.count("""
                SELECT COUNT(*) FROM dbo.Employees AS e JOIN dbo.Departments AS d ON d.DepartmentId = e.DepartmentId
                WHERE e.EmployeeId = ? AND e.EmployeeId <> ? AND (e.ManagerId = ? OR d.ManagerId = ?)""",
                employeeId, managerEmployeeId, managerEmployeeId, managerEmployeeId) > 0;
    }

    @Override
    public List<Long> managerChain(long employeeId) {
        return jdbc.query("""
                WITH Chain AS (
                    SELECT ManagerId, 1 AS Depth FROM dbo.Employees WHERE EmployeeId = ?
                    UNION ALL
                    SELECT e.ManagerId, c.Depth + 1 FROM Chain AS c JOIN dbo.Employees AS e ON e.EmployeeId = c.ManagerId
                    WHERE c.Depth < 50
                )
                SELECT ManagerId FROM Chain WHERE ManagerId IS NOT NULL ORDER BY Depth""", rs -> rs.getLong(1), employeeId);
    }

    @Override
    public List<EmployeeOption> options() {
        return jdbc.query("SELECT EmployeeId, EmployeeCode, FullName, DepartmentId FROM dbo.Employees "
                        + "WHERE Status <> 'TERMINATED' ORDER BY FullName",
                rs -> new EmployeeOption(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4)));
    }
}
