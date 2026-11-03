package com.hrm.testsupport;

import com.hrm.model.Department;
import com.hrm.model.Employee;
import com.hrm.model.EmployeeForm;
import com.hrm.model.EmployeeStatus;
import com.hrm.model.PayrollSettings;
import com.hrm.model.Position;
import com.hrm.model.TaxBracket;
import com.hrm.security.AuthUser;
import com.hrm.security.Role;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared test data. "Today" is fixed to Monday 2026-11-02 (Asia/Ho_Chi_Minh) so date rules are
 * deterministic; payroll parameters are the 2026 values seeded in db/seed.sql.
 */
public final class Fixtures {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final LocalDate TODAY = LocalDate.of(2026, 11, 2);

    private Fixtures() {
    }

    public static Clock clockAt(LocalDateTime time) {
        return Clock.fixed(time.atZone(ZONE).toInstant(), ZONE);
    }

    /** 2026-11-02 09:00 local time. */
    public static Clock clock() {
        return clockAt(TODAY.atTime(9, 0));
    }

    public static BigDecimal vnd(long amount) {
        return BigDecimal.valueOf(amount);
    }

    // ------------------------------------------------------------------ users

    public static AuthUser admin() {
        return new AuthUser(1, "admin", null, "Quản trị hệ thống", EnumSet.of(Role.ADMIN));
    }

    public static AuthUser hr(long employeeId) {
        return new AuthUser(2, "hr", employeeId, "Lê Thị Lan", EnumSet.of(Role.HR, Role.EMPLOYEE));
    }

    public static AuthUser manager(long employeeId) {
        return new AuthUser(10 + employeeId, "manager" + employeeId, employeeId, "Trưởng nhóm", EnumSet.of(Role.MANAGER, Role.EMPLOYEE));
    }

    public static AuthUser employee(long employeeId) {
        return new AuthUser(100 + employeeId, "emp" + employeeId, employeeId, "Nhân viên", EnumSet.of(Role.EMPLOYEE));
    }

    /** Technical account without an employee record (e.g. the API client). */
    public static AuthUser technical() {
        return new AuthUser(0, AuthUser.API_CLIENT, null, "Hệ thống tích hợp", EnumSet.of(Role.HR));
    }

    // ------------------------------------------------------------------ organisation

    public static Department department(long id, String code, boolean active) {
        Department d = new Department();
        d.setDepartmentId(id);
        d.setDepartmentCode(code);
        d.setDepartmentName("Phòng " + code);
        d.setActive(active);
        return d;
    }

    public static Position position(long id, String code, long minSalary, long maxSalary) {
        Position p = new Position();
        p.setPositionId(id);
        p.setPositionCode(code);
        p.setPositionName("Lập trình viên");
        p.setJobLevel(4);
        p.setMinSalary(vnd(minSalary));
        p.setMaxSalary(vnd(maxSalary));
        p.setActive(true);
        return p;
    }

    public static Employee employeeRecord(long id, String code) {
        Employee e = new Employee();
        e.setEmployeeId(id);
        e.setEmployeeCode(code);
        e.setFullName("Nhân viên " + code);
        e.setGender("M");
        e.setEmail(code.toLowerCase(Locale.ROOT) + "@example.com");
        e.setDepartmentId(4L);
        e.setPositionId(6L);
        e.setHireDate(LocalDate.of(2024, 1, 15));
        e.setStatus(EmployeeStatus.ACTIVE);
        e.setBaseSalary(vnd(20_000_000));
        e.setAllowance(vnd(1_000_000));
        e.setDependents(1);
        e.setNationalId("001090000001");
        e.setBankAccount("0011000000001");
        return e;
    }

    public static EmployeeFormBuilder employeeForm() {
        return new EmployeeFormBuilder();
    }

    // ------------------------------------------------------------------ payroll

    public static PayrollSettings payrollSettings() {
        return payrollSettings(Map.of());
    }

    public static PayrollSettings payrollSettings(Map<String, BigDecimal> overrides) {
        Map<String, BigDecimal> values = new HashMap<>(Map.of(
                PayrollSettings.SOCIAL_INSURANCE_RATE, new BigDecimal("0.08"),
                PayrollSettings.HEALTH_INSURANCE_RATE, new BigDecimal("0.015"),
                PayrollSettings.UNEMPLOYMENT_INSURANCE_RATE, new BigDecimal("0.01"),
                PayrollSettings.BASE_SALARY_REFERENCE, vnd(2_340_000),
                PayrollSettings.REGIONAL_MINIMUM_WAGE, vnd(5_310_000),
                PayrollSettings.INSURANCE_CAP_MULTIPLIER, BigDecimal.valueOf(20),
                PayrollSettings.PERSONAL_DEDUCTION, vnd(15_500_000),
                PayrollSettings.DEPENDENT_DEDUCTION, vnd(6_200_000),
                PayrollSettings.UNPAID_DAYS_NO_INSURANCE, BigDecimal.valueOf(14)));
        values.putAll(overrides);
        return new PayrollSettings(values, List.of(
                bracket(1, 0, 5_000_000L, "0.05"),
                bracket(2, 5_000_000, 10_000_000L, "0.10"),
                bracket(3, 10_000_000, 18_000_000L, "0.15"),
                bracket(4, 18_000_000, 32_000_000L, "0.20"),
                bracket(5, 32_000_000, 52_000_000L, "0.25"),
                bracket(6, 52_000_000, 80_000_000L, "0.30"),
                bracket(7, 80_000_000, null, "0.35")));
    }

    private static TaxBracket bracket(int no, long lower, Long upper, String rate) {
        return new TaxBracket(no, vnd(lower), upper == null ? null : vnd(upper), new BigDecimal(rate));
    }

    /** Valid employee form (IT department #4, developer #6 with band 12–28 M); tweak one field per test. */
    public static final class EmployeeFormBuilder {
        private String employeeCode = "EMP0100";
        private String fullName = "Nguyễn Văn Kiểm";
        private String gender = "M";
        private LocalDate dateOfBirth = LocalDate.of(1995, 5, 20);
        private String email = "kiem.nv@example.com";
        private String phone = "0912345678";
        private String nationalId = "001095012345";
        private String taxCode;
        private Long departmentId = 4L;
        private Long positionId = 6L;
        private Long managerId;
        private LocalDate hireDate = LocalDate.of(2026, 10, 1);
        private LocalDate terminationDate;
        private EmployeeStatus status = EmployeeStatus.PROBATION;
        private BigDecimal baseSalary = vnd(15_000_000);
        private BigDecimal allowance = vnd(1_000_000);
        private Integer dependents = 0;

        public EmployeeFormBuilder code(String v) { employeeCode = v; return this; }
        public EmployeeFormBuilder fullName(String v) { fullName = v; return this; }
        public EmployeeFormBuilder gender(String v) { gender = v; return this; }
        public EmployeeFormBuilder dateOfBirth(LocalDate v) { dateOfBirth = v; return this; }
        public EmployeeFormBuilder email(String v) { email = v; return this; }
        public EmployeeFormBuilder phone(String v) { phone = v; return this; }
        public EmployeeFormBuilder nationalId(String v) { nationalId = v; return this; }
        public EmployeeFormBuilder taxCode(String v) { taxCode = v; return this; }
        public EmployeeFormBuilder departmentId(Long v) { departmentId = v; return this; }
        public EmployeeFormBuilder positionId(Long v) { positionId = v; return this; }
        public EmployeeFormBuilder managerId(Long v) { managerId = v; return this; }
        public EmployeeFormBuilder hireDate(LocalDate v) { hireDate = v; return this; }
        public EmployeeFormBuilder terminationDate(LocalDate v) { terminationDate = v; return this; }
        public EmployeeFormBuilder status(EmployeeStatus v) { status = v; return this; }
        public EmployeeFormBuilder baseSalary(BigDecimal v) { baseSalary = v; return this; }
        public EmployeeFormBuilder allowance(BigDecimal v) { allowance = v; return this; }
        public EmployeeFormBuilder dependents(Integer v) { dependents = v; return this; }

        public EmployeeForm build() {
            return new EmployeeForm(employeeCode, fullName, gender, dateOfBirth, email, phone, null, nationalId, taxCode,
                    null, null, departmentId, positionId, managerId, hireDate, terminationDate, status, baseSalary,
                    allowance, dependents);
        }
    }
}
