package com.hrm.service;

import com.hrm.dao.AuditDao;
import com.hrm.model.AuditLog;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class AuditService {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditDao auditDao;

    public AuditService(AuditDao auditDao) {
        this.auditDao = auditDao;
    }

    public void record(String action, String entityType, Object entityId, String details) {
        record(UserContext.currentUsername(), action, entityType, entityId, details);
    }

    public void record(String username, String action, String entityType, Object entityId, String details) {
        String id = entityId == null ? null : String.valueOf(entityId);
        String ip = UserContext.clientIp();
        audit.info("{} user={} {}#{} {} ip={}", action, username, entityType, id, details == null ? "" : details, ip);
        try {
            auditDao.insert(username, action, entityType, id, details, ip);
        } catch (RuntimeException e) {
            log.error("Could not persist audit entry {} for {}", action, username, e);
        }
    }

    public Page<AuditLog> search(String keyword, String action, LocalDate from, LocalDate to, PageRequest page) {
        return auditDao.search(keyword, action, from, to, page);
    }
}
