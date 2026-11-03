package com.hrm.service;

import com.hrm.model.PayrollSettings;
import com.hrm.service.PayrollCalculator.Input;
import com.hrm.service.PayrollCalculator.Result;
import com.hrm.testsupport.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Map;

import static com.hrm.testsupport.Fixtures.vnd;
import static org.assertj.core.api.Assertions.assertThat;

class PayrollCalculatorTest {

    private static final BigDecimal DAYS_22 = BigDecimal.valueOf(22);

    private final PayrollCalculator calculator = new PayrollCalculator();
    private final PayrollSettings settings = Fixtures.payrollSettings();

    private Result month(long baseSalary, long allowance, int dependents, int workedDays, int paidLeaveDays) {
        return calculator.calculate(new Input(vnd(baseSalary), vnd(allowance), dependents, DAYS_22,
                BigDecimal.valueOf(workedDays), BigDecimal.valueOf(paidLeaveDays)), settings);
    }

    @Test
    @DisplayName("30 M, 1 dependant, full month: insurance 3,150,000 · PIT 265,000 · net 26,585,000")
    void fullMonthMatchesHandCalculation() {
        Result r = calculator.simulate(vnd(30_000_000), BigDecimal.ZERO, 1, settings);

        assertThat(r.grossSalary()).isEqualByComparingTo("30000000");
        assertThat(r.socialInsurance()).isEqualByComparingTo("2400000");
        assertThat(r.healthInsurance()).isEqualByComparingTo("450000");
        assertThat(r.unemploymentInsurance()).isEqualByComparingTo("300000");
        assertThat(r.totalInsurance()).isEqualByComparingTo("3150000");
        assertThat(r.dependentDeduction()).isEqualByComparingTo("6200000");
        assertThat(r.taxableIncome()).isEqualByComparingTo("5150000");
        assertThat(r.personalIncomeTax()).isEqualByComparingTo("265000");
        assertThat(r.netSalary()).isEqualByComparingTo("26585000");
    }

    @ParameterizedTest(name = "taxable {0} → PIT {1}")
    @CsvSource({
            "0, 0",
            "5000000, 250000",
            "5000001, 250000",
            "10000000, 750000",
            "18000000, 1950000",
            "32000000, 4750000",
            "52000000, 9750000",
            "80000000, 18150000",
            "100000000, 25150000"
    })
    void progressiveTaxOverSevenBrackets(long taxable, long expectedTax) {
        assertThat(calculator.tax(vnd(taxable), settings)).isEqualByComparingTo(vnd(expectedTax));
    }

    @Test
    void lowIncomeBelowPersonalDeductionPaysNoTax() {
        Result r = month(10_000_000, 0, 0, 22, 0);

        assertThat(r.totalInsurance()).isEqualByComparingTo("1050000");
        assertThat(r.taxableIncome()).isZero();
        assertThat(r.personalIncomeTax()).isZero();
        assertThat(r.netSalary()).isEqualByComparingTo("8950000");
    }

    @Test
    void dependantsReduceTaxableIncomeButNeverBelowZero() {
        Result r = month(30_000_000, 0, 2, 22, 0);

        assertThat(r.dependentDeduction()).isEqualByComparingTo("12400000");
        assertThat(r.taxableIncome()).isZero();
        assertThat(r.netSalary()).isEqualByComparingTo("26850000");
    }

    @Nested
    @DisplayName("insurance caps (20 × reference salary / regional minimum wage)")
    class InsuranceCaps {

        @Test
        void socialAndHealthInsuranceAreCappedAt46_8Million() {
            Result r = month(60_000_000, 0, 0, 22, 0);

            assertThat(r.socialInsurance()).isEqualByComparingTo("3744000");
            assertThat(r.healthInsurance()).isEqualByComparingTo("702000");
            assertThat(r.unemploymentInsurance()).isEqualByComparingTo("600000");
            assertThat(r.personalIncomeTax()).isEqualByComparingTo("6613500");
            assertThat(r.netSalary()).isEqualByComparingTo("48340500");
        }

        @Test
        void unemploymentInsuranceIsCappedAt106_2Million() {
            Result r = month(120_000_000, 0, 0, 22, 0);

            assertThat(r.unemploymentInsurance()).isEqualByComparingTo("1062000");
        }
    }

    @Nested
    @DisplayName("working days")
    class WorkingDays {

        @Test
        void salaryAndAllowanceAreProratedByPaidDays() {
            Result r = month(22_000_000, 2_200_000, 0, 11, 0);

            assertThat(r.paidDays()).isEqualByComparingTo("11");
            assertThat(r.unpaidDays()).isEqualByComparingTo("11");
            assertThat(r.salaryAmount()).isEqualByComparingTo("11000000");
            assertThat(r.allowanceAmount()).isEqualByComparingTo("1100000");
            assertThat(r.totalInsurance()).as("insurance is on the contract salary").isEqualByComparingTo("2310000");
            assertThat(r.netSalary()).isEqualByComparingTo("9790000");
        }

        @Test
        void paidLeaveCountsAsWorkedButPaidDaysNeverExceedTheStandard() {
            assertThat(month(22_000_000, 0, 0, 20, 2).salaryAmount()).isEqualByComparingTo("22000000");
            Result over = month(22_000_000, 0, 0, 22, 3);
            assertThat(over.paidDays()).isEqualByComparingTo("22");
            assertThat(over.salaryAmount()).isEqualByComparingTo("22000000");
        }

        @Test
        void fourteenUnpaidDaysOrMoreMeansNoInsurance() {
            Result thirteen = month(22_000_000, 0, 0, 9, 0);
            Result fourteen = month(22_000_000, 0, 0, 8, 0);

            assertThat(thirteen.totalInsurance()).isPositive();
            assertThat(fourteen.socialInsurance()).isZero();
            assertThat(fourteen.healthInsurance()).isZero();
            assertThat(fourteen.unemploymentInsurance()).isZero();
            assertThat(fourteen.netSalary()).isEqualByComparingTo("8000000");
        }

        @Test
        void noInsuranceWhenItWouldExceedGrossIncome() {
            PayrollSettings noDayRule = Fixtures.payrollSettings(
                    Map.of(PayrollSettings.UNPAID_DAYS_NO_INSURANCE, BigDecimal.valueOf(31)));
            Result r = calculator.calculate(new Input(vnd(30_000_000), BigDecimal.ZERO, 0, DAYS_22, BigDecimal.ONE,
                    BigDecimal.ZERO), noDayRule);

            assertThat(r.grossSalary()).isEqualByComparingTo("1363636");
            assertThat(r.totalInsurance()).isZero();
            assertThat(r.netSalary()).isEqualByComparingTo("1363636");
        }

        @Test
        void zeroStandardDaysPaysNothingInsteadOfDividingByZero() {
            Result r = calculator.calculate(new Input(vnd(30_000_000), vnd(1_000_000), 0, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO), settings);

            assertThat(r.grossSalary()).isZero();
            assertThat(r.netSalary()).isZero();
        }
    }

    @Test
    @DisplayName("amounts are rounded HALF_UP like T-SQL ROUND(x, 0), not banker's rounding")
    void roundsHalfUpLikeTheStoredProcedure() {
        Result r = calculator.calculate(new Input(vnd(1_000_001), BigDecimal.ZERO, 0, BigDecimal.valueOf(2),
                BigDecimal.ONE, BigDecimal.ZERO), settings);

        assertThat(r.salaryAmount()).isEqualByComparingTo("500001");
    }
}
