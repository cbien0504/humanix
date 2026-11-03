package com.hrm.model;

import java.time.LocalDate;

public record LeaveRequestForm(Long leaveTypeId, LocalDate startDate, LocalDate endDate, String reason) {
}
