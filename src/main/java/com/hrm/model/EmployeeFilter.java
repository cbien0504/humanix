package com.hrm.model;

public record EmployeeFilter(String keyword, Long departmentId, Long positionId, EmployeeStatus status,
                             Long teamOfManagerId) {

    public EmployeeFilter withTeamOf(Long managerEmployeeId) {
        return new EmployeeFilter(keyword, departmentId, positionId, status, managerEmployeeId);
    }
}
