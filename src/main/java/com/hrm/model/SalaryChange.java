package com.hrm.model;

import java.math.BigDecimal;
import java.time.Instant;

/** One row of SalaryHistory (written by trigger trg_Employees_AuditSalary). */
public record SalaryChange(BigDecimal oldBaseSalary, BigDecimal newBaseSalary, BigDecimal oldAllowance,
                           BigDecimal newAllowance, String changedBy, Instant changedAt) {
}
