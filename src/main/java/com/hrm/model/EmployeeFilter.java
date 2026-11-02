package com.hrm.model;

/**
 * Employee search criteria. {@code teamOfManagerId} restricts results to the team of a
 * manager (direct reports + departments he/she manages) – set by the service, never by clients.
 */
public record EmployeeFilter(String keyword, Long departmentId, Long positionId, EmployeeStatus status,
                             Long teamOfManagerId) {

    public EmployeeFilter withTeamOf(Long managerEmployeeId) {
        return new EmployeeFilter(keyword, departmentId, positionId, status, managerEmployeeId);
    }
}
