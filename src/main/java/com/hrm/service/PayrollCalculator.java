package com.hrm.service;

import com.hrm.model.PayrollSettings;
import com.hrm.model.TaxBracket;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure payroll formula (BR-32 … BR-34) – the Java twin of sp_CalculateMonthlyPayroll.
 * <ul>
 *   <li>paid days = min(standard days, worked days + paid leave days)</li>
 *   <li>salary / allowance prorated by paid days; gross = salary + allowance</li>
 *   <li>employee insurance 8% + 1.5% capped at {@code cap × reference salary}, 1% capped at
 *       {@code cap × regional minimum wage}; none when unpaid days ≥ threshold or insurance &gt; gross</li>
 *   <li>taxable = max(0, gross − insurance − personal − dependants × dependent deduction)</li>
 *   <li>progressive PIT over {@link TaxBracket}s; net = gross − insurance − PIT</li>
 * </ul>
 * Every amount is rounded to whole VND with HALF_UP – the same as T-SQL {@code ROUND(x, 0)}
 * for positive values – so both implementations produce identical payslips (verified by the
 * integration test PayrollProcedureParityIT).
 */
public final class PayrollCalculator {

    private static final int SCALE = 10;

    /** Inputs of one employee for one month. */
    public record Input(BigDecimal baseSalary, BigDecimal allowance, int dependents, BigDecimal standardDays,
                        BigDecimal workedDays, BigDecimal paidLeaveDays) {
    }

    /** Full breakdown of a payslip. */
    public record Result(BigDecimal paidDays, BigDecimal unpaidDays, BigDecimal salaryAmount, BigDecimal allowanceAmount,
                         BigDecimal grossSalary, BigDecimal socialInsurance, BigDecimal healthInsurance,
                         BigDecimal unemploymentInsurance, BigDecimal totalInsurance, BigDecimal personalDeduction,
                         BigDecimal dependentDeduction, BigDecimal taxableIncome, BigDecimal personalIncomeTax,
                         BigDecimal netSalary) {
    }

    public Result calculate(Input in, PayrollSettings s) {
        BigDecimal std = in.standardDays();
        BigDecimal worked = in.workedDays().add(in.paidLeaveDays());
        BigDecimal paidDays = worked.min(std);
        BigDecimal unpaidDays = std.subtract(paidDays);

        BigDecimal salary = prorate(in.baseSalary(), paidDays, std);
        BigDecimal allowance = prorate(in.allowance(), paidDays, std);
        BigDecimal gross = salary.add(allowance);

        BigDecimal cap = s.get(PayrollSettings.INSURANCE_CAP_MULTIPLIER);
        BigDecimal siHiBase = in.baseSalary().min(cap.multiply(s.get(PayrollSettings.BASE_SALARY_REFERENCE)));
        BigDecimal uiBase = in.baseSalary().min(cap.multiply(s.get(PayrollSettings.REGIONAL_MINIMUM_WAGE)));
        BigDecimal si = round(siHiBase.multiply(s.get(PayrollSettings.SOCIAL_INSURANCE_RATE)));
        BigDecimal hi = round(siHiBase.multiply(s.get(PayrollSettings.HEALTH_INSURANCE_RATE)));
        BigDecimal ui = round(uiBase.multiply(s.get(PayrollSettings.UNEMPLOYMENT_INSURANCE_RATE)));
        boolean insured = unpaidDays.compareTo(s.get(PayrollSettings.UNPAID_DAYS_NO_INSURANCE)) < 0
                && si.add(hi).add(ui).compareTo(gross) <= 0;
        if (!insured) {
            si = BigDecimal.ZERO;
            hi = BigDecimal.ZERO;
            ui = BigDecimal.ZERO;
        }
        BigDecimal insurance = si.add(hi).add(ui);

        BigDecimal personal = s.get(PayrollSettings.PERSONAL_DEDUCTION);
        BigDecimal dependent = s.get(PayrollSettings.DEPENDENT_DEDUCTION).multiply(BigDecimal.valueOf(in.dependents()));
        BigDecimal taxable = gross.subtract(insurance).subtract(personal).subtract(dependent).max(BigDecimal.ZERO);
        BigDecimal pit = tax(taxable, s);
        BigDecimal net = gross.subtract(insurance).subtract(pit);

        return new Result(paidDays, unpaidDays, salary, allowance, gross, si, hi, ui, insurance,
                round(personal), round(dependent), round(taxable), pit, net);
    }

    /** Progressive tax: sum of (min(income, upper) − lower) × rate over the brackets the income reaches. */
    public BigDecimal tax(BigDecimal taxableIncome, PayrollSettings s) {
        BigDecimal total = BigDecimal.ZERO;
        for (TaxBracket b : s.taxBrackets()) {
            if (taxableIncome.compareTo(b.lowerBound()) <= 0) {
                continue;
            }
            BigDecimal top = b.upperBound() == null ? taxableIncome : taxableIncome.min(b.upperBound());
            total = total.add(top.subtract(b.lowerBound()).multiply(b.rate()));
        }
        return round(total);
    }

    /** Gross→net simulation for a full month (UC-35). */
    public Result simulate(BigDecimal baseSalary, BigDecimal allowance, int dependents, PayrollSettings s) {
        BigDecimal days = BigDecimal.valueOf(22);
        return calculate(new Input(baseSalary, allowance, dependents, days, days, BigDecimal.ZERO), s);
    }

    private static BigDecimal prorate(BigDecimal amount, BigDecimal paidDays, BigDecimal standardDays) {
        if (standardDays.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return round(amount.multiply(paidDays).divide(standardDays, SCALE, RoundingMode.HALF_UP));
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP);
    }
}
