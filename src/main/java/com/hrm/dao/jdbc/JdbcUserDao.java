package com.hrm.dao.jdbc;

import com.hrm.dao.UserDao;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.model.User;
import com.hrm.security.Role;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** JDBC implementation of {@link UserDao}. Roles are aggregated with STRING_AGG in one query. */
public class JdbcUserDao implements UserDao {

    private static final String SELECT = """
            SELECT u.UserId, u.Username, u.PasswordHash, u.EmployeeId, e.EmployeeCode, e.FullName,
                   u.IsActive, u.MustChangePassword, u.FailedLoginCount, u.LockedUntil, u.LastLoginAt, u.CreatedAt,
                   (SELECT STRING_AGG(r.RoleCode, ',') FROM dbo.UserRoles ur JOIN dbo.Roles r ON r.RoleId = ur.RoleId
                    WHERE ur.UserId = u.UserId) AS RoleCodes
            FROM dbo.Users AS u
            LEFT JOIN dbo.Employees AS e ON e.EmployeeId = u.EmployeeId
            """;

    private static final Map<String, String> SORT = Map.of(
            "username", "u.Username", "fullName", "e.FullName", "lastLogin", "u.LastLoginAt", "createdAt", "u.CreatedAt");

    private final JdbcExecutor jdbc;

    public JdbcUserDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    static User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getLong("UserId"));
        u.setUsername(rs.getString("Username"));
        u.setPasswordHash(rs.getString("PasswordHash"));
        u.setEmployeeId(Rows.longValue(rs, "EmployeeId"));
        u.setEmployeeCode(rs.getString("EmployeeCode"));
        u.setFullName(rs.getString("FullName"));
        u.setActive(rs.getBoolean("IsActive"));
        u.setMustChangePassword(rs.getBoolean("MustChangePassword"));
        u.setFailedLoginCount(rs.getInt("FailedLoginCount"));
        u.setLockedUntil(Rows.instant(rs, "LockedUntil"));
        u.setLastLoginAt(Rows.instant(rs, "LastLoginAt"));
        u.setCreatedAt(Rows.instant(rs, "CreatedAt"));
        Set<Role> roles = EnumSet.noneOf(Role.class);
        String codes = rs.getString("RoleCodes");
        if (codes != null) {
            for (String code : codes.split(",")) {
                roles.add(Role.valueOf(code.trim()));
            }
        }
        u.setRoles(roles);
        return u;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jdbc.queryOne(SELECT + " WHERE u.Username = ?", JdbcUserDao::map, username);
    }

    @Override
    public Optional<User> findById(long userId) {
        return jdbc.queryOne(SELECT + " WHERE u.UserId = ?", JdbcUserDao::map, userId);
    }

    @Override
    public Page<User> search(String keyword, Role role, Boolean active, PageRequest page) {
        SqlWhere where = new SqlWhere()
                .andContains(keyword, "u.Username", "e.FullName", "e.EmployeeCode")
                .andIfPresent("u.IsActive = ?", active)
                .andIf(role != null, "EXISTS (SELECT 1 FROM dbo.UserRoles ur JOIN dbo.Roles r ON r.RoleId = ur.RoleId "
                        + "WHERE ur.UserId = u.UserId AND r.RoleCode = ?)", role == null ? null : role.name());
        List<User> items = jdbc.query(SELECT + where.toSql() + SqlOrder.of(SORT, page, "u.Username", "u.UserId")
                        + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", JdbcUserDao::map,
                where.paramsWith(page.offset(), page.size()));
        long total = jdbc.count("SELECT COUNT(*) FROM dbo.Users AS u LEFT JOIN dbo.Employees AS e "
                + "ON e.EmployeeId = u.EmployeeId" + where.toSql(), where.params().toArray());
        return Page.of(items, page, total);
    }

    @Override
    public long create(String username, String passwordHash, Long employeeId, boolean mustChangePassword) {
        return jdbc.insert("INSERT INTO dbo.Users (Username, PasswordHash, EmployeeId, MustChangePassword) VALUES (?, ?, ?, ?)",
                username, passwordHash, employeeId, mustChangePassword);
    }

    @Override
    public void replaceRoles(long userId, Set<Role> roles) {
        jdbc.update("DELETE FROM dbo.UserRoles WHERE UserId = ?", userId);
        List<Object[]> rows = new ArrayList<>();
        for (Role role : roles) {
            rows.add(new Object[]{userId, role.name()});
        }
        jdbc.batch("INSERT INTO dbo.UserRoles (UserId, RoleId) SELECT ?, RoleId FROM dbo.Roles WHERE RoleCode = ?", rows);
    }

    @Override
    public void setActive(long userId, boolean active) {
        jdbc.update("UPDATE dbo.Users SET IsActive = ?, FailedLoginCount = 0, LockedUntil = NULL, "
                + "UpdatedAt = SYSUTCDATETIME() WHERE UserId = ?", active, userId);
    }

    @Override
    public void updatePassword(long userId, String passwordHash, boolean mustChangePassword) {
        jdbc.update("UPDATE dbo.Users SET PasswordHash = ?, MustChangePassword = ?, FailedLoginCount = 0, "
                + "LockedUntil = NULL, UpdatedAt = SYSUTCDATETIME() WHERE UserId = ?", passwordHash, mustChangePassword, userId);
    }

    @Override
    public void recordLoginSuccess(long userId, Instant at) {
        jdbc.update("UPDATE dbo.Users SET FailedLoginCount = 0, LockedUntil = NULL, LastLoginAt = ? WHERE UserId = ?",
                at, userId);
    }

    @Override
    public int recordLoginFailure(long userId, int maxFailures, Instant lockUntil) {
        // single statement: no lost update when two failed logins race
        Integer count = jdbc.queryOne("""
                UPDATE dbo.Users
                   SET FailedLoginCount = FailedLoginCount + 1,
                       LockedUntil = CASE WHEN FailedLoginCount + 1 >= ? THEN ? ELSE LockedUntil END
                OUTPUT inserted.FailedLoginCount
                WHERE UserId = ?""", rs -> rs.getInt(1), maxFailures, lockUntil, userId).orElse(0);
        return count;
    }

    @Override
    public void unlock(long userId) {
        jdbc.update("UPDATE dbo.Users SET FailedLoginCount = 0, LockedUntil = NULL WHERE UserId = ?", userId);
    }

    @Override
    public boolean existsByEmployeeId(long employeeId) {
        return jdbc.count("SELECT COUNT(*) FROM dbo.Users WHERE EmployeeId = ?", employeeId) > 0;
    }

    @Override
    public List<Map<String, Object>> findEmployeesWithoutAccount() {
        return jdbc.query("""
                SELECT e.EmployeeId, e.EmployeeCode, e.FullName FROM dbo.Employees AS e
                WHERE e.Status <> 'TERMINATED' AND NOT EXISTS (SELECT 1 FROM dbo.Users u WHERE u.EmployeeId = e.EmployeeId)
                ORDER BY e.EmployeeCode""", rs -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("employeeId", rs.getLong("EmployeeId"));
            m.put("employeeCode", rs.getString("EmployeeCode"));
            m.put("fullName", rs.getString("FullName"));
            return m;
        });
    }
}
