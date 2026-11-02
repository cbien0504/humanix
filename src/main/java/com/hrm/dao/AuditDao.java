package com.hrm.dao;

import com.hrm.model.AuditLog;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;

import java.time.LocalDate;

/** Append-only audit trail. */
public interface AuditDao {

    void insert(String username, String action, String entityType, String entityId, String details, String ip);

    Page<AuditLog> search(String keyword, String action, LocalDate from, LocalDate to, PageRequest page);
}
