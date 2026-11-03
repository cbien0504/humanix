package com.hrm.service;

import com.hrm.model.AttendanceStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AttendancePolicyTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 2);

    private final AttendancePolicy policy = new AttendancePolicy(LocalTime.of(8, 30), 5, 4);

    @ParameterizedTest(name = "check-in {0} → {1}, {2} minutes late")
    @CsvSource({
            "07:45, PRESENT, 0",
            "08:30, PRESENT, 0",
            "08:35, PRESENT, 0",
            "08:36, LATE, 6",
            "10:05, LATE, 95"
    })
    void checkInStatus(LocalTime time, AttendanceStatus status, int lateMinutes) {
        AttendancePolicy.CheckInResult r = policy.evaluateCheckIn(time);

        assertThat(r.status()).isEqualTo(status);
        assertThat(r.lateMinutes()).isEqualTo(lateMinutes);
    }

    @ParameterizedTest(name = "{0}–{1} → {2}")
    @CsvSource({
            "08:20, 17:30, PRESENT",
            "08:50, 17:40, LATE",
            "08:00, 11:30, HALF_DAY",
            "08:00, 12:00, PRESENT",
            "09:00, 12:00, HALF_DAY"
    })
    void finalStatusOfADay(LocalTime in, LocalTime out, AttendanceStatus expected) {
        assertThat(policy.evaluateDay(DAY.atTime(in), DAY.atTime(out))).isEqualTo(expected);
    }

    @Test
    void withoutCheckOutOnlyTheCheckInCounts() {
        assertThat(policy.evaluateDay(LocalDateTime.of(DAY, LocalTime.of(8, 40)), null)).isEqualTo(AttendanceStatus.LATE);
    }

    @Test
    void workingDaysSkipWeekendsAndHolidays() {
        LocalDate first = LocalDate.of(2026, 11, 1);
        LocalDate last = LocalDate.of(2026, 11, 30);
        LocalDate weekdayHoliday = LocalDate.of(2026, 11, 20);
        LocalDate weekendHoliday = LocalDate.of(2026, 11, 21);

        assertThat(new WorkingDayCalculator(List.of()).count(first, last)).isEqualTo(21);
        assertThat(new WorkingDayCalculator(List.of(weekdayHoliday, weekendHoliday)).count(first, last)).isEqualTo(20);
        assertThat(new WorkingDayCalculator(List.of(weekdayHoliday)).isWorkingDay(weekdayHoliday)).isFalse();
        assertThat(new WorkingDayCalculator(List.of()).workingDays(last, first)).isEmpty();
    }
}
