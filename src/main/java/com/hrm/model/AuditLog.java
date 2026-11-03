package com.hrm.model;

import java.time.Instant;

public record AuditLog(long auditId, String username, String action, String entityType, String entityId,
                       String details, String ipAddress, Instant createdAt) {
}
