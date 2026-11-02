package com.hrm.model;

import java.math.BigDecimal;

/** Row of sp_GetDepartmentStatistics. */
public record DepartmentStat(long departmentId, String departmentCode, String departmentName, String managerName,
                             int headcount, int activeCount, int probationCount, int onLeaveCount, int maleCount,
                             int femaleCount, BigDecimal avgBaseSalary, BigDecimal monthlySalaryFund,
                             BigDecimal avgYearsOfService, BigDecimal avgAge, int newHiresYtd, int terminationsYtd,
                             BigDecimal turnoverRate) {
}
