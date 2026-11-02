package com.hrm.dao.jdbc;

import com.hrm.dao.DepartmentDao;
import com.hrm.model.Department;
import com.hrm.model.DepartmentForm;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class JdbcDepartmentDao implements DepartmentDao {

    private static final String SELECT = """
            SELECT d.DepartmentId, d.DepartmentCode, d.DepartmentName, d.Description, d.ManagerId,
                   m.FullName AS ManagerName, d.IsActive, d.CreatedAt,
                   (SELECT COUNT(*) FROM dbo.Employees AS e
                    WHERE e.DepartmentId = d.DepartmentId AND e.Status <> 'TERMINATED') AS EmployeeCount
            FROM dbo.Departments AS d
            LEFT JOIN dbo.Employees AS m ON m.EmployeeId = d.ManagerId
            """;

    private final JdbcExecutor jdbc;

    public JdbcDepartmentDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    private static Department map(ResultSet rs) throws SQLException {
        Department d = new Department();
        d.setDepartmentId(rs.getLong("DepartmentId"));
        d.setDepartmentCode(rs.getString("DepartmentCode"));
        d.setDepartmentName(rs.getString("DepartmentName"));
        d.setDescription(rs.getString("Description"));
        d.setManagerId(Rows.longValue(rs, "ManagerId"));
        d.setManagerName(rs.getString("ManagerName"));
        d.setActive(rs.getBoolean("IsActive"));
        d.setCreatedAt(Rows.instant(rs, "CreatedAt"));
        d.setEmployeeCount(rs.getInt("EmployeeCount"));
        return d;
    }

    @Override
    public List<Department> findAll() {
        return jdbc.query(SELECT + " ORDER BY d.DepartmentName", JdbcDepartmentDao::map);
    }

    @Override
    public Optional<Department> findById(long id) {
        return jdbc.queryOne(SELECT + " WHERE d.DepartmentId = ?", JdbcDepartmentDao::map, id);
    }

    @Override
    public Optional<Department> findByCode(String code) {
        return jdbc.queryOne(SELECT + " WHERE d.DepartmentCode = ?", JdbcDepartmentDao::map, code);
    }

    @Override
    public long insert(DepartmentForm f) {
        return jdbc.insert("INSERT INTO dbo.Departments (DepartmentCode, DepartmentName, Description, ManagerId, IsActive) "
                + "VALUES (?, ?, ?, ?, ?)", f.departmentCode(), f.departmentName(), f.description(), f.managerId(),
                f.active() == null || f.active());
    }

    @Override
    public void update(long id, DepartmentForm f) {
        jdbc.update("UPDATE dbo.Departments SET DepartmentCode = ?, DepartmentName = ?, Description = ?, ManagerId = ?, "
                + "IsActive = ?, UpdatedAt = SYSUTCDATETIME() WHERE DepartmentId = ?", f.departmentCode(), f.departmentName(),
                f.description(), f.managerId(), f.active() == null || f.active(), id);
    }

    @Override
    public void delete(long id) {
        jdbc.update("DELETE FROM dbo.Departments WHERE DepartmentId = ?", id);
    }
}
