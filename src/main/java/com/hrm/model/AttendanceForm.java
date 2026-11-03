package com.hrm.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record AttendanceForm(Long employeeId, String employeeCode, LocalDate workDate, LocalTime checkIn,
                             LocalTime checkOut, AttendanceStatus status, String note) {
}
