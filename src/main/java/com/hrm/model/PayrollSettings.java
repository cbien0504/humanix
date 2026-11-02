package com.hrm.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Payroll parameters (table PayrollSettings) and the tax schedule (TaxBrackets). */
public record PayrollSettings(Map<String, BigDecimal> values, List<TaxBracket> taxBrackets) {

    public static final String SOCIAL_INSURANCE_RATE = "SOCIAL_INSURANCE_RATE";
    public static final String HEALTH_INSURANCE_RATE = "HEALTH_INSURANCE_RATE";
    public static final String UNEMPLOYMENT_INSURANCE_RATE = "UNEMPLOYMENT_INSURANCE_RATE";
    public static final String BASE_SALARY_REFERENCE = "BASE_SALARY_REFERENCE";
    public static final String REGIONAL_MINIMUM_WAGE = "REGIONAL_MINIMUM_WAGE";
    public static final String INSURANCE_CAP_MULTIPLIER = "INSURANCE_CAP_MULTIPLIER";
    public static final String PERSONAL_DEDUCTION = "PERSONAL_DEDUCTION";
    public static final String DEPENDENT_DEDUCTION = "DEPENDENT_DEDUCTION";
    public static final String UNPAID_DAYS_NO_INSURANCE = "UNPAID_DAYS_NO_INSURANCE";

    public static final List<String> REQUIRED_KEYS = List.of(SOCIAL_INSURANCE_RATE, HEALTH_INSURANCE_RATE,
            UNEMPLOYMENT_INSURANCE_RATE, BASE_SALARY_REFERENCE, REGIONAL_MINIMUM_WAGE, INSURANCE_CAP_MULTIPLIER,
            PERSONAL_DEDUCTION, DEPENDENT_DEDUCTION, UNPAID_DAYS_NO_INSURANCE);

    public BigDecimal get(String key) {
        BigDecimal v = values.get(key);
        if (v == null) {
            throw new IllegalStateException("Missing payroll setting " + key);
        }
        return v;
    }
}
