package com.hrm.model;

import java.math.BigDecimal;

/**
 * Leave balance of one type and year. {@code available = entitled − used − pending}: days of
 * pending requests are reserved so an employee cannot over-request (BR-22).
 */
public record LeaveBalance(long leaveTypeId, String leaveTypeCode, String leaveTypeName, int year,
                           BigDecimal entitledDays, BigDecimal usedDays, BigDecimal pendingDays) {

    public BigDecimal getRemainingDays() {
        return entitledDays.subtract(usedDays);
    }

    public BigDecimal getAvailableDays() {
        return entitledDays.subtract(usedDays).subtract(pendingDays);
    }
}
