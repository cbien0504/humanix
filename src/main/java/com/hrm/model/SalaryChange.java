package com.hrm.model;

import java.math.BigDecimal;
import java.time.Instant;

public record SalaryChange(BigDecimal oldBaseSalary, BigDecimal newBaseSalary, BigDecimal oldAllowance,
                           BigDecimal newAllowance, String changedBy, Instant changedAt) {
}
