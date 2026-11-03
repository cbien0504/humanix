package com.hrm.security;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class AccessPolicy {

    private record Rule(String prefix, Set<String> methods, Set<Role> roles) {
        boolean matches(String method, String path) {
            boolean pathMatches = path.equals(prefix) || path.startsWith(prefix.endsWith("/") ? prefix : prefix + "/");
            return pathMatches && (methods.isEmpty() || methods.contains(method));
        }
    }

    private static final Set<String> ANY = Set.of();
    private static final Set<String> READ = Set.of("GET");
    private static final Set<String> WRITE = Set.of("POST", "PUT", "DELETE", "PATCH");

    private static final List<String> API_CLIENT_PATHS = List.of(
            "/api/employees", "/api/attendance", "/api/departments", "/api/positions");

    private final List<Rule> rules = new ArrayList<>();

    public AccessPolicy() {
        allow("/admin", ANY, Role.ADMIN);
        allow("/api/admin", ANY, Role.ADMIN);
        allow("/api/users", ANY, Role.ADMIN);
        allow("/api/audit-logs", ANY, Role.ADMIN);
        allow("/api/payroll/settings", WRITE, Role.ADMIN);
        allow("/departments", ANY, Role.HR);
        allow("/positions", ANY, Role.HR);
        allow("/api/departments", WRITE, Role.HR);
        allow("/api/positions", WRITE, Role.HR);
        allow("/employees", ANY, Role.HR, Role.MANAGER);
        allow("/api/employees", WRITE, Role.HR);
        allow("/api/employees", READ, Role.HR, Role.MANAGER);
        allow("/import", ANY, Role.HR);
        allow("/api/import", ANY, Role.HR);
        allow("/attendance/manage", ANY, Role.HR, Role.MANAGER);
        allow("/leave/approvals", ANY, Role.HR, Role.MANAGER);
        allow("/api/attendance/check-in", ANY, Role.EMPLOYEE);
        allow("/api/attendance/check-out", ANY, Role.EMPLOYEE);
        allow("/api/attendance", WRITE, Role.HR);
        allow("/api/leave-requests/balances/init", ANY, Role.HR);
        allow("/payroll/my", ANY, Role.EMPLOYEE);
        allow("/api/payroll/my", ANY, Role.EMPLOYEE);
        allow("/payroll/payslip", ANY, Role.EMPLOYEE);
        allow("/api/payroll/payslips", ANY, Role.EMPLOYEE);
        allow("/api/payroll/simulate", ANY, Role.EMPLOYEE, Role.HR);
        allow("/payroll", ANY, Role.HR);
        allow("/api/payroll", ANY, Role.HR);
        allow("/recruitment", ANY, Role.HR, Role.MANAGER);
        allow("/api/jobs", WRITE, Role.HR);
        allow("/api/jobs", READ, Role.HR, Role.MANAGER);
        allow("/api/candidates", ANY, Role.HR, Role.MANAGER);
        allow("/performance/manage", ANY, Role.HR, Role.MANAGER);
        allow("/api/review-cycles", WRITE, Role.HR);
        allow("/reports/employees", ANY, Role.HR);
        allow("/reports", ANY, Role.HR, Role.MANAGER);
        allow("/api/reports", ANY, Role.HR, Role.MANAGER);
    }

    private void allow(String prefix, Set<String> methods, Role... roles) {
        rules.add(new Rule(prefix, methods, roles.length == 0 ? Set.of() : EnumSet.of(roles[0], roles)));
    }

    public boolean isAllowed(AuthUser user, String method, String path) {
        if (AuthUser.API_CLIENT.equals(user.getUsername())) {
            return API_CLIENT_PATHS.stream().anyMatch(p -> path.equals(p) || path.startsWith(p + "/"));
        }
        for (Rule rule : rules) {
            if (rule.matches(method, path)) {
                return user.hasAnyRole(rule.roles());
            }
        }
        return true;
    }
}
