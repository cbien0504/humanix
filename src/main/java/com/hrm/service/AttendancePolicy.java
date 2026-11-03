package com.hrm.service;

import com.hrm.model.AttendanceStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class AttendancePolicy {

    private final LocalTime workStart;
    private final int graceMinutes;
    private final double halfDayMaxHours;

    public AttendancePolicy(LocalTime workStart, int graceMinutes, double halfDayMaxHours) {
        this.workStart = workStart;
        this.graceMinutes = graceMinutes;
        this.halfDayMaxHours = halfDayMaxHours;
    }

    public record CheckInResult(AttendanceStatus status, int lateMinutes) {
    }

    public CheckInResult evaluateCheckIn(LocalTime time) {
        LocalTime limit = workStart.plusMinutes(graceMinutes);
        if (time.isAfter(limit)) {
            int late = (int) Duration.between(workStart, time).toMinutes();
            return new CheckInResult(AttendanceStatus.LATE, late);
        }
        return new CheckInResult(AttendanceStatus.PRESENT, 0);
    }

    public AttendanceStatus evaluateDay(LocalDateTime checkIn, LocalDateTime checkOut) {
        CheckInResult in = evaluateCheckIn(checkIn.toLocalTime());
        if (checkOut != null) {
            double hours = Duration.between(checkIn, checkOut).toMinutes() / 60.0;
            if (hours < halfDayMaxHours) {
                return AttendanceStatus.HALF_DAY;
            }
        }
        return in.status();
    }

    public LocalTime getWorkStart() {
        return workStart;
    }

    public int getGraceMinutes() {
        return graceMinutes;
    }
}
