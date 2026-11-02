package com.hrm.dao.jdbc;

import com.hrm.dao.PayrollDao;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.model.PayrollPeriod;
import com.hrm.model.PayrollSettings;
import com.hrm.model.PayrollStatus;
import com.hrm.model.Payslip;
import com.hrm.model.SettingDefinition;
import com.hrm.model.TaxBracket;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcPayrollDao implements PayrollDao {

    private static final String PERIOD_SELECT = """
            SELECT PeriodId, PeriodYear, PeriodMonth, StandardWorkDays, Status, EmployeeCount, TotalGross, TotalNet,
                   CalculatedAt, CalculatedBy, ApprovedAt, ApprovedBy, PaidAt
            FROM dbo.PayrollPeriods
            """;

    private static final Map<String, String> SORT = Map.of(
            "code", "EmployeeCode", "name", "FullName", "department", "DepartmentName", "gross", "GrossSalary",
            "net", "NetSalary", "tax", "PersonalIncomeTax");

    private final JdbcExecutor jdbc;

    public JdbcPayrollDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    static PayrollPeriod mapPeriod(ResultSet rs) throws SQLException {
        PayrollPeriod p = new PayrollPeriod();
        p.setPeriodId(rs.getLong("PeriodId"));
        p.setPeriodYear(rs.getInt("PeriodYear"));
        p.setPeriodMonth(rs.getInt("PeriodMonth"));
        p.setStandardWorkDays(rs.getBigDecimal("StandardWorkDays"));
        p.setStatus(PayrollStatus.valueOf(rs.getString("Status")));
        p.setEmployeeCount(Rows.integer(rs, "EmployeeCount"));
        p.setTotalGross(rs.getBigDecimal("TotalGross"));
        p.setTotalNet(rs.getBigDecimal("TotalNet"));
        p.setCalculatedAt(Rows.instant(rs, "CalculatedAt"));
        p.setCalculatedBy(rs.getString("CalculatedBy"));
        p.setApprovedAt(Rows.instant(rs, "ApprovedAt"));
        p.setApprovedBy(rs.getString("ApprovedBy"));
        p.setPaidAt(Rows.instant(rs, "PaidAt"));
        return p;
    }

    static Payslip mapPayslip(ResultSet rs) throws SQLException {
        Payslip p = new Payslip();
        p.setPayrollId(rs.getLong("PayrollId"));
        p.setPeriodId(rs.getLong("PeriodId"));
        p.setPeriodYear(rs.getInt("PeriodYear"));
        p.setPeriodMonth(rs.getInt("PeriodMonth"));
        p.setPeriodStatus(PayrollStatus.valueOf(rs.getString("PeriodStatus")));
        p.setEmployeeId(rs.getLong("EmployeeId"));
        p.setEmployeeCode(rs.getString("EmployeeCode"));
        p.setFullName(rs.getString("FullName"));
        p.setDepartmentName(rs.getString("DepartmentName"));
        p.setPositionName(rs.getString("PositionName"));
        p.setBankAccount(rs.getString("BankAccount"));
        p.setBankName(rs.getString("BankName"));
        p.setBaseSalary(rs.getBigDecimal("BaseSalary"));
        p.setAllowance(rs.getBigDecimal("Allowance"));
        p.setDependents(rs.getInt("Dependents"));
        p.setStandardDays(rs.getBigDecimal("StandardDays"));
        p.setWorkedDays(rs.getBigDecimal("WorkedDays"));
        p.setPaidLeaveDays(rs.getBigDecimal("PaidLeaveDays"));
        p.setPaidDays(rs.getBigDecimal("PaidDays"));
        p.setUnpaidDays(rs.getBigDecimal("UnpaidDays"));
        p.setSalaryAmount(rs.getBigDecimal("SalaryAmount"));
        p.setAllowanceAmount(rs.getBigDecimal("AllowanceAmount"));
        p.setGrossSalary(rs.getBigDecimal("GrossSalary"));
        p.setSocialInsurance(rs.getBigDecimal("SocialInsurance"));
        p.setHealthInsurance(rs.getBigDecimal("HealthInsurance"));
        p.setUnemploymentInsurance(rs.getBigDecimal("UnemploymentInsurance"));
        p.setTotalInsurance(rs.getBigDecimal("TotalInsurance"));
        p.setPersonalDeduction(rs.getBigDecimal("PersonalDeduction"));
        p.setDependentDeduction(rs.getBigDecimal("DependentDeduction"));
        p.setTaxableIncome(rs.getBigDecimal("TaxableIncome"));
        p.setPersonalIncomeTax(rs.getBigDecimal("PersonalIncomeTax"));
        p.setNetSalary(rs.getBigDecimal("NetSalary"));
        return p;
    }

    @Override
    public List<PayrollPeriod> periods(Integer year) {
        if (year == null) {
            return jdbc.query(PERIOD_SELECT + " ORDER BY PeriodYear DESC, PeriodMonth DESC", JdbcPayrollDao::mapPeriod);
        }
        return jdbc.query(PERIOD_SELECT + " WHERE PeriodYear = ? ORDER BY PeriodMonth DESC", JdbcPayrollDao::mapPeriod, year);
    }

    @Override
    public Optional<PayrollPeriod> period(long periodId) {
        return jdbc.queryOne(PERIOD_SELECT + " WHERE PeriodId = ?", JdbcPayrollDao::mapPeriod, periodId);
    }

    @Override
    public PayrollPeriod calculate(int year, int month, String calculatedBy) {
        long periodId = jdbc.call("{call dbo.sp_CalculateMonthlyPayroll(?, ?, ?)}", rs -> rs.getLong("PeriodId"),
                year, month, calculatedBy).stream().findFirst().orElseThrow();
        return period(periodId).orElseThrow();
    }

    @Override
    public int changeStatus(long periodId, PayrollStatus expected, PayrollStatus target, String username) {
        if (target == PayrollStatus.APPROVED) {
            return jdbc.update("UPDATE dbo.PayrollPeriods SET Status = ?, ApprovedAt = SYSUTCDATETIME(), ApprovedBy = ? "
                    + "WHERE PeriodId = ? AND Status = ?", target, username, periodId, expected);
        }
        if (target == PayrollStatus.PAID) {
            return jdbc.update("UPDATE dbo.PayrollPeriods SET Status = ?, PaidAt = SYSUTCDATETIME() "
                    + "WHERE PeriodId = ? AND Status = ?", target, periodId, expected);
        }
        return jdbc.update("UPDATE dbo.PayrollPeriods SET Status = ? WHERE PeriodId = ? AND Status = ?", target, periodId, expected);
    }

    @Override
    public Page<Payslip> payslips(long periodId, String keyword, Long departmentId, PageRequest page) {
        SqlWhere where = new SqlWhere().and("PeriodId = ?", periodId)
                .andContains(keyword, "EmployeeCode", "FullName")
                .andIfPresent("DepartmentId = ?", departmentId);
        List<Payslip> items = jdbc.query("SELECT * FROM dbo.vw_PayrollDetails" + where.toSql()
                        + SqlOrder.of(SORT, page, "EmployeeCode", "PayrollId") + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                JdbcPayrollDao::mapPayslip, where.paramsWith(page.offset(), page.size()));
        long total = jdbc.count("SELECT COUNT(*) FROM dbo.vw_PayrollDetails" + where.toSql(), where.params().toArray());
        return Page.of(items, page, total);
    }

    @Override
    public List<Payslip> allPayslips(long periodId) {
        return jdbc.query("SELECT * FROM dbo.vw_PayrollDetails WHERE PeriodId = ? ORDER BY DepartmentName, EmployeeCode",
                JdbcPayrollDao::mapPayslip, periodId);
    }

    @Override
    public Optional<Payslip> payslip(long payrollId) {
        return jdbc.queryOne("SELECT * FROM dbo.vw_PayrollDetails WHERE PayrollId = ?", JdbcPayrollDao::mapPayslip, payrollId);
    }

    @Override
    public List<Payslip> payslipsOfEmployee(long employeeId) {
        return jdbc.query("SELECT * FROM dbo.vw_PayrollDetails WHERE EmployeeId = ? AND PeriodStatus IN ('APPROVED', 'PAID') "
                + "ORDER BY PeriodYear DESC, PeriodMonth DESC", JdbcPayrollDao::mapPayslip, employeeId);
    }

    @Override
    public List<Map<String, Object>> departmentTotals(long periodId) {
        return jdbc.query("""
                SELECT DepartmentName, COUNT(*) AS Employees, SUM(GrossSalary) AS Gross, SUM(TotalInsurance) AS Insurance,
                       SUM(PersonalIncomeTax) AS Tax, SUM(NetSalary) AS Net
                FROM dbo.vw_PayrollDetails WHERE PeriodId = ? GROUP BY DepartmentName ORDER BY SUM(GrossSalary) DESC""", rs -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("departmentName", rs.getString("DepartmentName"));
            m.put("employees", rs.getInt("Employees"));
            m.put("gross", rs.getBigDecimal("Gross"));
            m.put("insurance", rs.getBigDecimal("Insurance"));
            m.put("tax", rs.getBigDecimal("Tax"));
            m.put("net", rs.getBigDecimal("Net"));
            return m;
        }, periodId);
    }

    @Override
    public PayrollSettings settings() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        for (SettingDefinition d : settingDefinitions()) {
            values.put(d.key(), d.value());
        }
        List<TaxBracket> brackets = jdbc.query("SELECT BracketNo, LowerBound, UpperBound, Rate FROM dbo.TaxBrackets ORDER BY BracketNo",
                rs -> new TaxBracket(rs.getInt(1), rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getBigDecimal(4)));
        return new PayrollSettings(values, brackets);
    }

    @Override
    public List<SettingDefinition> settingDefinitions() {
        return jdbc.query("SELECT SettingKey, SettingValue, Description FROM dbo.PayrollSettings ORDER BY SettingKey",
                rs -> new SettingDefinition(rs.getString(1), rs.getBigDecimal(2), rs.getString(3)));
    }

    @Override
    public void updateSetting(String key, BigDecimal value) {
        jdbc.update("UPDATE dbo.PayrollSettings SET SettingValue = ?, UpdatedAt = SYSUTCDATETIME() WHERE SettingKey = ?", value, key);
    }
}
