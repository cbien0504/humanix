package com.hrm.model;

import java.time.LocalDate;

/** New leave request submitted by an employee. */
public record LeaveRequestForm(Long leaveTypeId, LocalDate startDate, LocalDate endDate, String reason) {
}
