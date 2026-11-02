package com.hrm.dao;

import com.hrm.model.AttendanceFilter;
import com.hrm.model.AttendanceRecord;
import com.hrm.model.AttendanceStatus;
import com.hrm.model.MonthlyAttendance;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceDao {

    Optional<AttendanceRecord> find(long employeeId, LocalDate workDate);

    Optional<AttendanceRecord> findById(long attendanceId);

    long insert(long employeeId, LocalDate workDate, LocalDateTime checkIn, LocalDateTime checkOut,
                AttendanceStatus status, int lateMinutes, String source, String note);

    void update(long attendanceId, LocalDateTime checkIn, LocalDateTime checkOut, AttendanceStatus status,
                int lateMinutes, String source, String note);

    void delete(long attendanceId);

    Page<AttendanceRecord> search(AttendanceFilter filter, PageRequest page);

    List<AttendanceRecord> findByEmployee(long employeeId, LocalDate from, LocalDate to);

    Optional<MonthlyAttendance> monthly(long employeeId, int year, int month);

    long countCheckedIn(LocalDate workDate);
}
