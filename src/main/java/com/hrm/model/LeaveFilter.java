package com.hrm.model;

public record LeaveFilter(String keyword, LeaveStatus status, Long leaveTypeId, Integer year, Long employeeId,
                          Long teamOfManagerId) {

    public LeaveFilter scopedTo(Long onlyEmployeeId, Long managerEmployeeId) {
        return new LeaveFilter(keyword, status, leaveTypeId, year, onlyEmployeeId != null ? onlyEmployeeId : employeeId,
                managerEmployeeId);
    }
}
