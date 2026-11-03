package com.hrm.model;

import java.math.BigDecimal;

public record LeaveType(long leaveTypeId, String leaveTypeCode, String leaveTypeName, boolean paid,
                        boolean requiresBalance, BigDecimal maxDaysPerRequest, boolean active) {
}
