package com.hrm.dao.jdbc;

import com.hrm.dao.PositionDao;
import com.hrm.model.Position;
import com.hrm.model.PositionForm;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class JdbcPositionDao implements PositionDao {

    private static final String SELECT = """
            SELECT p.PositionId, p.PositionCode, p.PositionName, p.JobLevel, p.MinSalary, p.MaxSalary, p.Description, p.IsActive,
                   (SELECT COUNT(*) FROM dbo.Employees AS e
                    WHERE e.PositionId = p.PositionId AND e.Status <> 'TERMINATED') AS EmployeeCount
            FROM dbo.Positions AS p
            """;

    private final JdbcExecutor jdbc;

    public JdbcPositionDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    private static Position map(ResultSet rs) throws SQLException {
        Position p = new Position();
        p.setPositionId(rs.getLong("PositionId"));
        p.setPositionCode(rs.getString("PositionCode"));
        p.setPositionName(rs.getString("PositionName"));
        p.setJobLevel(rs.getInt("JobLevel"));
        p.setMinSalary(rs.getBigDecimal("MinSalary"));
        p.setMaxSalary(rs.getBigDecimal("MaxSalary"));
        p.setDescription(rs.getString("Description"));
        p.setActive(rs.getBoolean("IsActive"));
        p.setEmployeeCount(rs.getInt("EmployeeCount"));
        return p;
    }

    @Override
    public List<Position> findAll() {
        return jdbc.query(SELECT + " ORDER BY p.JobLevel DESC, p.PositionName", JdbcPositionDao::map);
    }

    @Override
    public Optional<Position> findById(long id) {
        return jdbc.queryOne(SELECT + " WHERE p.PositionId = ?", JdbcPositionDao::map, id);
    }

    @Override
    public Optional<Position> findByCode(String code) {
        return jdbc.queryOne(SELECT + " WHERE p.PositionCode = ?", JdbcPositionDao::map, code);
    }

    @Override
    public long insert(PositionForm f) {
        return jdbc.insert("INSERT INTO dbo.Positions (PositionCode, PositionName, JobLevel, MinSalary, MaxSalary, Description, IsActive) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)", f.positionCode(), f.positionName(), f.jobLevel(), f.minSalary(), f.maxSalary(),
                f.description(), f.active() == null || f.active());
    }

    @Override
    public void update(long id, PositionForm f) {
        jdbc.update("UPDATE dbo.Positions SET PositionCode = ?, PositionName = ?, JobLevel = ?, MinSalary = ?, MaxSalary = ?, "
                + "Description = ?, IsActive = ?, UpdatedAt = SYSUTCDATETIME() WHERE PositionId = ?", f.positionCode(),
                f.positionName(), f.jobLevel(), f.minSalary(), f.maxSalary(), f.description(), f.active() == null || f.active(), id);
    }

    @Override
    public void delete(long id) {
        jdbc.update("DELETE FROM dbo.Positions WHERE PositionId = ?", id);
    }
}
