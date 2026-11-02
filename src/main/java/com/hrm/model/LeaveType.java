package com.hrm.model;

import java.math.BigDecimal;

/** Leave type (table LeaveTypes). */
public record LeaveType(long leaveTypeId, String leaveTypeCode, String leaveTypeName, boolean paid,
                        boolean requiresBalance, BigDecimal maxDaysPerRequest, boolean active) {
}
