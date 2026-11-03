package com.hrm.service;

import com.hrm.dao.PayrollDao;
import com.hrm.exception.BusinessException;
import com.hrm.exception.ForbiddenException;
import com.hrm.exception.NotFoundException;
import com.hrm.exception.ValidationException;
import com.hrm.model.PayrollPeriod;
import com.hrm.model.PayrollSettings;
import com.hrm.model.PayrollStatus;
import com.hrm.model.Payslip;
import com.hrm.report.TabularReport;
import com.hrm.service.PayrollService.SimulationRequest;
import com.hrm.testsupport.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.hrm.testsupport.Fixtures.vnd;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private PayrollDao payrollDao;
    @Mock
    private AuditService audit;

    private PayrollService service;

    @BeforeEach
    void setUp() {
        service = new PayrollService(payrollDao, new PayrollCalculator(), audit, Fixtures.clock());
    }

    private static PayrollPeriod period(long id, int month, PayrollStatus status) {
        PayrollPeriod p = new PayrollPeriod();
        p.setPeriodId(id);
        p.setPeriodYear(2026);
        p.setPeriodMonth(month);
        p.setStatus(status);
        p.setStandardWorkDays(BigDecimal.valueOf(22));
        p.setEmployeeCount(36);
        p.setTotalGross(vnd(800_000_000));
        p.setTotalNet(vnd(700_000_000));
        p.setCalculatedAt(Instant.parse("2026-11-01T02:00:00Z"));
        p.setCalculatedBy("hr");
        return p;
    }

    private static Payslip payslip(long id, long employeeId, PayrollStatus periodStatus) {
        Payslip s = new Payslip();
        s.setPayrollId(id);
        s.setEmployeeId(employeeId);
        s.setEmployeeCode("EMP00" + employeeId);
        s.setPeriodStatus(periodStatus);
        s.setNetSalary(vnd(26_585_000));
        return s;
    }

    @Nested
    class Calculate {

        @Test
        void refusesAMonthInTheFuture() {
            assertThatThrownBy(() -> service.calculate(Fixtures.hr(5), 2026, 12))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("tương lai");
            verifyNoInteractions(payrollDao);
        }

        @Test
        void refusesAnInvalidMonth() {
            assertThatThrownBy(() -> service.calculate(Fixtures.hr(5), 2026, 13))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        void runsTheProcedureForTheCurrentMonthAndAudits() {
            when(payrollDao.calculate(2026, 11, "hr")).thenReturn(period(9, 11, PayrollStatus.CALCULATED));

            PayrollPeriod result = service.calculate(Fixtures.hr(5), 2026, 11);

            assertThat(result.getPeriodId()).isEqualTo(9);
            verify(audit).record(eq("PAYROLL_CALCULATED"), eq("PayrollPeriod"), eq(9L), contains("11/2026"));
        }
    }

    @Nested
    class Workflow {

        @Test
        void approvesACalculatedPeriod() {
            when(payrollDao.period(3)).thenReturn(Optional.of(period(3, 10, PayrollStatus.CALCULATED)),
                    Optional.of(period(3, 10, PayrollStatus.APPROVED)));
            when(payrollDao.changeStatus(3, PayrollStatus.CALCULATED, PayrollStatus.APPROVED, "hr")).thenReturn(1);

            PayrollPeriod approved = service.approve(Fixtures.hr(5), 3);

            assertThat(approved.getStatus()).isEqualTo(PayrollStatus.APPROVED);
            verify(audit).record("PAYROLL_APPROVED", "PayrollPeriod", 3L, "10/2026");
        }

        @Test
        void cannotApproveAPeriodThatWasNotCalculated() {
            when(payrollDao.period(3)).thenReturn(Optional.of(period(3, 10, PayrollStatus.OPEN)));

            assertThatThrownBy(() -> service.approve(Fixtures.hr(5), 3)).isInstanceOf(BusinessException.class);
            verify(payrollDao, never()).changeStatus(anyLong(), any(), any(), anyString());
        }

        @Test
        void concurrentApprovalIsDetectedByTheOptimisticUpdate() {
            when(payrollDao.period(3)).thenReturn(Optional.of(period(3, 10, PayrollStatus.CALCULATED)));
            when(payrollDao.changeStatus(3, PayrollStatus.CALCULATED, PayrollStatus.APPROVED, "hr")).thenReturn(0);

            assertThatThrownBy(() -> service.approve(Fixtures.hr(5), 3)).isInstanceOf(BusinessException.class);
            verifyNoInteractions(audit);
        }

        @Test
        void onlyApprovedPeriodsCanBeMarkedPaid() {
            when(payrollDao.period(3)).thenReturn(Optional.of(period(3, 10, PayrollStatus.CALCULATED)));

            assertThatThrownBy(() -> service.markPaid(Fixtures.hr(5), 3))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("đã được duyệt");
        }

        @Test
        void unknownPeriodIsNotFound() {
            when(payrollDao.period(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.period(404)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class PayslipConfidentiality {

        @Test
        void hrSeesAnyPayslip() {
            when(payrollDao.payslip(1)).thenReturn(Optional.of(payslip(1, 16, PayrollStatus.CALCULATED)));

            assertThat(service.payslip(Fixtures.hr(5), 1).getPayrollId()).isEqualTo(1);
        }

        @Test
        void ownerSeesHisApprovedPayslip() {
            when(payrollDao.payslip(1)).thenReturn(Optional.of(payslip(1, 16, PayrollStatus.PAID)));

            assertThat(service.payslip(Fixtures.employee(16), 1).getEmployeeId()).isEqualTo(16);
        }

        @Test
        void ownerCannotSeeAPayslipBeforeApproval() {
            when(payrollDao.payslip(1)).thenReturn(Optional.of(payslip(1, 16, PayrollStatus.CALCULATED)));

            assertThatThrownBy(() -> service.payslip(Fixtures.employee(16), 1))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("chưa được duyệt");
        }

        @Test
        void colleaguesAndManagersCannotSeeSomeoneElsesPayslip() {
            when(payrollDao.payslip(1)).thenReturn(Optional.of(payslip(1, 16, PayrollStatus.PAID)));

            assertThatThrownBy(() -> service.payslip(Fixtures.employee(17), 1)).isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> service.payslip(Fixtures.manager(11), 1)).isInstanceOf(ForbiddenException.class);
        }

        @Test
        void accountWithoutEmployeeHasNoPayslips() {
            assertThatThrownBy(() -> service.myPayslips(Fixtures.admin())).isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    class Simulator {

        @Test
        void estimatesNetSalaryWithCurrentParameters() {
            when(payrollDao.settings()).thenReturn(Fixtures.payrollSettings());

            var r = service.simulate(new SimulationRequest(vnd(30_000_000), null, 1));

            assertThat(r.netSalary()).isEqualByComparingTo("26585000");
        }

        @Test
        void rejectsNegativeSalaryAndTooManyDependants() {
            assertThatThrownBy(() -> service.simulate(new SimulationRequest(vnd(-1), vnd(-5), 21)))
                    .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getFieldErrors())
                            .containsKeys("baseSalary", "allowance", "dependents"));
            verifyNoInteractions(payrollDao);
        }
    }

    @Nested
    class Settings {

        @Test
        void rejectsUnknownKeysNegativeValuesAndRatesGivenInPercent() {
            when(payrollDao.settings()).thenReturn(Fixtures.payrollSettings());
            Map<String, BigDecimal> changes = new LinkedHashMap<>();
            changes.put("BONUS_RATE", BigDecimal.ONE);
            changes.put(PayrollSettings.PERSONAL_DEDUCTION, vnd(-1));
            changes.put(PayrollSettings.SOCIAL_INSURANCE_RATE, BigDecimal.valueOf(8));

            assertThatThrownBy(() -> service.updateSettings(changes))
                    .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getFieldErrors())
                            .containsKeys("BONUS_RATE", PayrollSettings.PERSONAL_DEDUCTION, PayrollSettings.SOCIAL_INSURANCE_RATE));
            verify(payrollDao, never()).updateSetting(anyString(), any());
        }

        @Test
        void writesAndAuditsOnlyTheValuesThatChanged() {
            when(payrollDao.settings()).thenReturn(Fixtures.payrollSettings());
            Map<String, BigDecimal> changes = new LinkedHashMap<>();
            changes.put(PayrollSettings.PERSONAL_DEDUCTION, new BigDecimal("15500000.00"));
            changes.put(PayrollSettings.DEPENDENT_DEDUCTION, vnd(6_500_000));

            service.updateSettings(changes);

            verify(payrollDao).updateSetting(PayrollSettings.DEPENDENT_DEDUCTION, vnd(6_500_000));
            verify(payrollDao, never()).updateSetting(eq(PayrollSettings.PERSONAL_DEDUCTION), any());
            verify(audit).record(eq("PAYROLL_SETTINGS_UPDATED"), eq("PayrollSettings"), isNull(), contains("DEPENDENT_DEDUCTION"));
        }
    }

    @Test
    void payrollSheetHasOneRowPerPayslipAndIsAudited() {
        when(payrollDao.period(3)).thenReturn(Optional.of(period(3, 10, PayrollStatus.APPROVED)));
        when(payrollDao.allPayslips(3)).thenReturn(List.of(payslip(1, 16, PayrollStatus.APPROVED),
                payslip(2, 17, PayrollStatus.APPROVED)));

        TabularReport sheet = service.payrollSheet(3);

        assertThat(sheet.title()).isEqualTo("BẢNG LƯƠNG THÁNG 10/2026");
        assertThat(sheet.rows()).hasSize(2);
        assertThat(sheet.rows().get(0)).hasSameSizeAs(sheet.columns());
        assertThat(sheet.rows().get(1).get(1)).isEqualTo("EMP0017");
        verify(audit).record("PAYROLL_EXPORTED", "PayrollPeriod", 3L, "10/2026");
    }

    @Test
    void periodListIsDelegated() {
        when(payrollDao.periods(anyInt())).thenReturn(List.of(period(3, 10, PayrollStatus.PAID)));

        assertThat(service.periods(2026)).hasSize(1);
    }
}
