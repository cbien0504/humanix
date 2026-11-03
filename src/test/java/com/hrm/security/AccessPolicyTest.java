package com.hrm.security;

import com.hrm.testsupport.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class AccessPolicyTest {

    private final AccessPolicy policy = new AccessPolicy();

    private static AuthUser as(String role) {
        return switch (role) {
            case "ADMIN" -> Fixtures.admin();
            case "HR" -> new AuthUser(2, "hr", 5L, "HR", EnumSet.of(Role.HR, Role.EMPLOYEE));
            case "MANAGER" -> Fixtures.manager(11);
            case "API" -> Fixtures.technical();
            default -> Fixtures.employee(16);
        };
    }

    @ParameterizedTest(name = "{0} {1} {2} → {3}")
    @CsvSource({
            "EMPLOYEE, GET,    /attendance,                 true",
            "EMPLOYEE, POST,   /api/attendance/check-in,    true",
            "EMPLOYEE, GET,    /payroll/my,                 true",
            "EMPLOYEE, POST,   /api/payroll/simulate,       true",
            "EMPLOYEE, GET,    /payroll,                    false",
            "EMPLOYEE, GET,    /api/payroll/periods,        false",
            "EMPLOYEE, GET,    /employees,                  false",
            "EMPLOYEE, GET,    /api/employees,              false",
            "EMPLOYEE, GET,    /leave/approvals,            false",
            "EMPLOYEE, GET,    /recruitment,                false",
            "EMPLOYEE, GET,    /admin/users,                false",
            "MANAGER,  GET,    /employees,                  true",
            "MANAGER,  GET,    /api/employees/16,           true",
            "MANAGER,  PUT,    /api/employees/16,           false",
            "MANAGER,  GET,    /leave/approvals,            true",
            "MANAGER,  POST,   /api/attendance,             false",
            "MANAGER,  GET,    /api/jobs,                   true",
            "MANAGER,  POST,   /api/jobs,                   false",
            "MANAGER,  POST,   /api/review-cycles,          false",
            "MANAGER,  GET,    /reports,                    true",
            "MANAGER,  GET,    /reports/employees/export,   false",
            "MANAGER,  GET,    /payroll,                    false",
            "MANAGER,  GET,    /import,                     false",
            "HR,       PUT,    /api/employees/16,           true",
            "HR,       POST,   /api/attendance/bulk,        true",
            "HR,       GET,    /payroll,                    true",
            "HR,       POST,   /import/employees,           true",
            "HR,       GET,    /api/payroll/settings,       true",
            "HR,       PUT,    /api/payroll/settings,       false",
            "HR,       GET,    /admin/audit,                false",
            "ADMIN,    PUT,    /api/payroll/settings,       true",
            "ADMIN,    GET,    /api/audit-logs,             true",
            "ADMIN,    GET,    /payroll,                    true",
            "API,      GET,    /api/employees,              true",
            "API,      POST,   /api/attendance/bulk,        true",
            "API,      GET,    /api/positions/6,            true",
            "API,      GET,    /api/payroll/periods,        false",
            "API,      GET,    /api/users,                  false",
            "API,      GET,    /employees,                  false"
    })
    void permissionMatrix(String role, String method, String path, boolean allowed) {
        assertThat(policy.isAllowed(as(role), method, path)).isEqualTo(allowed);
    }

    @Test
    void prefixesMatchWholePathSegmentsOnly() {
        assertThat(policy.isAllowed(Fixtures.employee(16), "GET", "/payrollx")).isTrue();
        assertThat(policy.isAllowed(Fixtures.technical(), "GET", "/api/employees-export")).isFalse();
    }
}
