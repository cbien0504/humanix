package com.hrm.dao;

import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.model.PayrollPeriod;
import com.hrm.model.PayrollSettings;
import com.hrm.model.PayrollStatus;
import com.hrm.model.Payslip;
import com.hrm.model.SettingDefinition;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PayrollDao {

    List<PayrollPeriod> periods(Integer year);

    Optional<PayrollPeriod> period(long periodId);

    /** Runs sp_CalculateMonthlyPayroll and returns the period summary. */
    PayrollPeriod calculate(int year, int month, String calculatedBy);

    /** Moves the period from {@code expected} to {@code target}; returns updated rows (optimistic). */
    int changeStatus(long periodId, PayrollStatus expected, PayrollStatus target, String username);

    Page<Payslip> payslips(long periodId, String keyword, Long departmentId, PageRequest page);

    List<Payslip> allPayslips(long periodId);

    Optional<Payslip> payslip(long payrollId);

    /** Payslips of approved/paid periods of one employee, newest first. */
    List<Payslip> payslipsOfEmployee(long employeeId);

    /** Per-department totals of a period: department → [count, gross, net]. */
    List<Map<String, Object>> departmentTotals(long periodId);

    PayrollSettings settings();

    List<SettingDefinition> settingDefinitions();

    void updateSetting(String key, BigDecimal value);
}
