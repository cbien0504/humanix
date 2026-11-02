package com.hrm.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Manual attendance entry / correction by HR, or one punch record received from a time clock
 * (identified either by {@code employeeId} or {@code employeeCode}).
 * A null {@code status} means "derive it from the times".
 */
public record AttendanceForm(Long employeeId, String employeeCode, LocalDate workDate, LocalTime checkIn,
                             LocalTime checkOut, AttendanceStatus status, String note) {
}
