package com.hrm.model;

import java.time.Instant;

/** One row of the audit trail (table AuditLogs). */
public record AuditLog(long auditId, String username, String action, String entityType, String entityId,
                       String details, String ipAddress, Instant createdAt) {
}
