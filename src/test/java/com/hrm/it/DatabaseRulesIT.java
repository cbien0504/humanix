package com.hrm.it;

import com.hrm.dao.jdbc.JdbcDepartmentDao;
import com.hrm.dao.jdbc.JdbcEmployeeDao;
import com.hrm.dao.jdbc.JdbcExecutor;
import com.hrm.dao.jdbc.JdbcLeaveDao;
import com.hrm.dao.jdbc.JdbcPayrollDao;
import com.hrm.dao.jdbc.JdbcPositionDao;
import com.hrm.exception.BusinessException;
import com.hrm.exception.DataIntegrityException;
import com.hrm.model.Employee;
import com.hrm.model.EmployeeForm;
import com.hrm.model.EmployeeStatus;
import com.hrm.model.LeaveRequestForm;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The database is the last line of defence: constraints and triggers hold even when the service
 * layer is bypassed (scripts, a buggy future feature). Each case runs against the real schema
 * through the real DAOs and is rolled back.
 */
class DatabaseRulesIT {

    private static HikariDataSource dataSource;
    private static JdbcExecutor jdbc;
    private static JdbcEmployeeDao employees;

    @BeforeAll
    static void connect() {
        dataSource = ItDatabase.open();
        jdbc = ItDatabase.executor(dataSource);
        employees = new JdbcEmployeeDao(jdbc);
    }

    @AfterAll
    static void disconnect() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private static EmployeeForm newEmployee(String code, BigDecimal salary) {
        long it = new JdbcDepartmentDao(jdbc).findByCode("IT").orElseThrow().getDepartmentId();
        long dev = new JdbcPositionDao(jdbc).findByCode("DEV").orElseThrow().getPositionId();
        return new EmployeeForm(code, "Kiểm Thử Ràng Buộc", "M", LocalDate.of(1995, 1, 1),
                code.toLowerCase(Locale.ROOT) + ".it@example.com", null, null, null, null, null, null, it, dev, null,
                LocalDate.of(2026, 11, 1), null, EmployeeStatus.PROBATION, salary, BigDecimal.ZERO, 0);
    }

    @Test
    void negativeSalaryIsStoppedByTheCheckConstraintEvenWhenTheServiceIsBypassed() {
        ItDatabase.rollbackAfter(jdbc, () ->
                assertThatThrownBy(() -> employees.insert(newEmployee("EMP9001", new BigDecimal("-5000000"))))
                        .isInstanceOfSatisfying(DataIntegrityException.class, e -> {
                            assertThat(e.getConstraint()).isEqualTo("CK_Employees_Salary");
                            assertThat(e.getField()).isEqualTo("baseSalary");
                            assertThat(e.getHttpStatus()).isEqualTo(409);
                        }));
    }

    @Test
    void duplicateEmployeeCodeIsStoppedByTheUniqueKey() {
        ItDatabase.rollbackAfter(jdbc, () ->
                assertThatThrownBy(() -> employees.insert(newEmployee("EMP0001", new BigDecimal("15000000"))))
                        .isInstanceOfSatisfying(DataIntegrityException.class, e -> {
                            assertThat(e.getConstraint()).isEqualTo("UQ_Employees_EmployeeCode");
                            assertThat(e.getMessage()).isEqualTo("Mã nhân viên đã tồn tại.");
                        }));
    }

    @Test
    void overlappingLeaveIsStoppedByTheTrigger() {
        record Existing(long employeeId, long typeId, LocalDate start, LocalDate end) {
        }
        Existing existing = jdbc.queryOne("""
                SELECT TOP (1) EmployeeId, LeaveTypeId, StartDate, EndDate FROM dbo.LeaveRequests
                WHERE Status IN ('PENDING', 'APPROVED') ORDER BY LeaveRequestId""",
                rs -> new Existing(rs.getLong(1), rs.getLong(2), rs.getObject(3, LocalDate.class), rs.getObject(4, LocalDate.class)))
                .orElseThrow();

        ItDatabase.rollbackAfter(jdbc, () ->
                assertThatThrownBy(() -> new JdbcLeaveDao(jdbc).insert(existing.employeeId(),
                        new LeaveRequestForm(existing.typeId(), existing.start(), existing.end(), "IT: chồng lấn"), BigDecimal.ONE))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("chồng lấn"));
    }

    @Test
    void salaryChangesAreRecordedWithTheApplicationUser() {
        Employee e = employees.findByCode("EMP0016").orElseThrow();
        BigDecimal raised = e.getBaseSalary().add(new BigDecimal("1000000"));

        ItDatabase.rollbackAfter(jdbc, () -> {
            employees.update(e.getEmployeeId(), new EmployeeForm(e.getEmployeeCode(), e.getFullName(), e.getGender(),
                    e.getDateOfBirth(), e.getEmail(), e.getPhone(), e.getAddress(), e.getNationalId(), e.getTaxCode(),
                    e.getBankAccount(), e.getBankName(), e.getDepartmentId(), e.getPositionId(), e.getManagerId(),
                    e.getHireDate(), e.getTerminationDate(), e.getStatus(), raised, e.getAllowance(), e.getDependents()));

            String changedBy = jdbc.queryScalar("SELECT TOP (1) ChangedBy FROM dbo.SalaryHistory WHERE EmployeeId = ? "
                    + "ORDER BY SalaryHistoryId DESC", String.class, e.getEmployeeId());
            assertThat(changedBy).as("SESSION_CONTEXT app_user set by JdbcExecutor").isEqualTo(ItDatabase.AUDIT_USER);
        });
    }

    @Test
    void anApprovedOrPaidPayrollMonthCannotBeRecalculated() {
        Integer paidMonth = jdbc.queryScalar("SELECT TOP (1) PeriodMonth FROM dbo.PayrollPeriods WHERE PeriodYear = 2026 "
                + "AND Status = 'PAID' ORDER BY PeriodMonth", Integer.class);

        assertThatThrownBy(() -> new JdbcPayrollDao(jdbc).calculate(2026, paidMonth, ItDatabase.AUDIT_USER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không thể tính lại");
    }
}
