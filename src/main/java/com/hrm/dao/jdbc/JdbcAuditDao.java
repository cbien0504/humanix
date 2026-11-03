package com.hrm.dao.jdbc;

import com.hrm.dao.AuditDao;
import com.hrm.model.AuditLog;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public class JdbcAuditDao implements AuditDao {

    private final JdbcExecutor jdbc;
    private final ZoneId zone;

    public JdbcAuditDao(JdbcExecutor jdbc, ZoneId zone) {
        this.jdbc = jdbc;
        this.zone = zone;
    }

    @Override
    public void insert(String username, String action, String entityType, String entityId, String details, String ip) {
        jdbc.update("INSERT INTO dbo.AuditLogs (Username, Action, EntityType, EntityId, Details, IpAddress) VALUES (?, ?, ?, ?, ?, ?)",
                username, action, entityType, entityId, truncate(details, 2000), ip);
    }

    @Override
    public Page<AuditLog> search(String keyword, String action, LocalDate from, LocalDate to, PageRequest page) {
        SqlWhere where = new SqlWhere()
                .andContains(keyword, "Username", "Details", "EntityId")
                .andIfPresent("Action = ?", action)
                .andIfPresent("CreatedAt >= ?", from == null ? null : from.atStartOfDay(zone).toInstant())
                .andIfPresent("CreatedAt < ?", to == null ? null : to.plusDays(1).atTime(LocalTime.MIDNIGHT).atZone(zone).toInstant());
        List<AuditLog> items = jdbc.query("SELECT AuditId, Username, Action, EntityType, EntityId, Details, IpAddress, CreatedAt "
                        + "FROM dbo.AuditLogs" + where.toSql() + " ORDER BY CreatedAt DESC, AuditId DESC "
                        + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                rs -> new AuditLog(rs.getLong("AuditId"), rs.getString("Username"), rs.getString("Action"),
                        rs.getString("EntityType"), rs.getString("EntityId"), rs.getString("Details"),
                        rs.getString("IpAddress"), Rows.instant(rs, "CreatedAt")),
                where.paramsWith(page.offset(), page.size()));
        long total = jdbc.count("SELECT COUNT(*) FROM dbo.AuditLogs" + where.toSql(), where.params().toArray());
        return Page.of(items, page, total);
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
