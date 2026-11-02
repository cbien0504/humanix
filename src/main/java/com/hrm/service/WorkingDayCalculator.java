package com.hrm.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Working-day rule BR-13: Monday–Friday except public holidays. Same rule as the SQL function
 * dbo.fn_WorkingDays, used to count the days of a leave request before it is stored.
 */
public final class WorkingDayCalculator {

    private final Set<LocalDate> holidays;

    public WorkingDayCalculator(Collection<LocalDate> holidays) {
        this.holidays = new TreeSet<>(holidays);
    }

    public boolean isWorkingDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !holidays.contains(date);
    }

    /** Working days in the closed range; empty when {@code to} is before {@code from}. */
    public List<LocalDate> workingDays(LocalDate from, LocalDate to) {
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (isWorkingDay(d)) {
                days.add(d);
            }
        }
        return days;
    }

    public int count(LocalDate from, LocalDate to) {
        return workingDays(from, to).size();
    }
}
