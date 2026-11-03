package com.hrm.it;

import com.hrm.dao.jdbc.JdbcExecutor;
import com.hrm.dao.jdbc.JdbcPayrollDao;
import com.hrm.model.PayrollPeriod;
import com.hrm.model.PayrollSettings;
import com.hrm.model.Payslip;
import com.hrm.service.PayrollCalculator;
import com.hrm.service.PayrollCalculator.Input;
import com.hrm.service.PayrollCalculator.Result;
import com.zaxxer.hikari.HikariDataSource;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class PayrollProcedureParityIT {

    private static HikariDataSource dataSource;
    private static JdbcExecutor jdbc;

    @BeforeAll
    static void connect() {
        dataSource = ItDatabase.open();
        jdbc = ItDatabase.executor(dataSource);
    }

    @AfterAll
    static void disconnect() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @ParameterizedTest(name = "{0}/2026")
    @ValueSource(ints = {8, 9, 10})
    void procedureAndJavaCalculatorProduceIdenticalPayslips(int month) {
        JdbcPayrollDao payrollDao = new JdbcPayrollDao(jdbc);
        PayrollCalculator calculator = new PayrollCalculator();

        ItDatabase.rollbackAfter(jdbc, () -> {
            jdbc.update("UPDATE dbo.PayrollPeriods SET Status = 'CALCULATED' WHERE PeriodYear = 2026 AND PeriodMonth = ?",
                    month);
            PayrollPeriod period = payrollDao.calculate(2026, month, ItDatabase.AUDIT_USER);
            List<Payslip> payslips = payrollDao.allPayslips(period.getPeriodId());
            PayrollSettings settings = payrollDao.settings();
            assertThat(payslips).hasSizeGreaterThanOrEqualTo(30);

            SoftAssertions softly = new SoftAssertions();
            for (Payslip p : payslips) {
                Result java = calculator.calculate(new Input(p.getBaseSalary(), p.getAllowance(), p.getDependents(),
                        p.getStandardDays(), p.getWorkedDays(), p.getPaidLeaveDays()), settings);
                same(softly, p, "paidDays", Payslip::getPaidDays, java.paidDays());
                same(softly, p, "salaryAmount", Payslip::getSalaryAmount, java.salaryAmount());
                same(softly, p, "allowanceAmount", Payslip::getAllowanceAmount, java.allowanceAmount());
                same(softly, p, "grossSalary", Payslip::getGrossSalary, java.grossSalary());
                same(softly, p, "socialInsurance", Payslip::getSocialInsurance, java.socialInsurance());
                same(softly, p, "healthInsurance", Payslip::getHealthInsurance, java.healthInsurance());
                same(softly, p, "unemploymentInsurance", Payslip::getUnemploymentInsurance, java.unemploymentInsurance());
                same(softly, p, "taxableIncome", Payslip::getTaxableIncome, java.taxableIncome());
                same(softly, p, "personalIncomeTax", Payslip::getPersonalIncomeTax, java.personalIncomeTax());
                same(softly, p, "netSalary", Payslip::getNetSalary, java.netSalary());
            }
            softly.assertAll();
        });
    }

    private static void same(SoftAssertions softly, Payslip p, String field, Function<Payslip, BigDecimal> stored,
                             BigDecimal expected) {
        softly.assertThat(stored.apply(p)).as("%s %s", p.getEmployeeCode(), field).isEqualByComparingTo(expected);
    }
}
