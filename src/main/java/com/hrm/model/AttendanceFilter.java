package com.hrm.model;

import java.time.LocalDate;

/** Attendance search criteria; {@code employeeId}/{@code teamOfManagerId} narrow the scope (set by the service). */
public record AttendanceFilter(String keyword, Long departmentId, Long employeeId, LocalDate from, LocalDate to,
                               AttendanceStatus status, Long teamOfManagerId) {

    public AttendanceFilter scopedTo(Long onlyEmployeeId, Long managerEmployeeId) {
        return new AttendanceFilter(keyword, departmentId, onlyEmployeeId != null ? onlyEmployeeId : employeeId,
                from, to, status, managerEmployeeId);
    }
}
