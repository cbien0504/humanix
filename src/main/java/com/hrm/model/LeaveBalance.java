package com.hrm.model;

import java.math.BigDecimal;

public record LeaveBalance(long leaveTypeId, String leaveTypeCode, String leaveTypeName, int year,
                           BigDecimal entitledDays, BigDecimal usedDays, BigDecimal pendingDays) {

    public BigDecimal getRemainingDays() {
        return entitledDays.subtract(usedDays);
    }

    public BigDecimal getAvailableDays() {
        return entitledDays.subtract(usedDays).subtract(pendingDays);
    }
}
