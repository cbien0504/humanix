package com.hrm.model;

import java.math.BigDecimal;

/** One employee's payroll line of a period (vw_PayrollDetails). */
public class Payslip {

    private long payrollId;
    private long periodId;
    private int periodYear;
    private int periodMonth;
    private PayrollStatus periodStatus;
    private long employeeId;
    private String employeeCode;
    private String fullName;
    private String departmentName;
    private String positionName;
    private String bankAccount;
    private String bankName;
    private BigDecimal baseSalary;
    private BigDecimal allowance;
    private int dependents;
    private BigDecimal standardDays;
    private BigDecimal workedDays;
    private BigDecimal paidLeaveDays;
    private BigDecimal paidDays;
    private BigDecimal unpaidDays;
    private BigDecimal salaryAmount;
    private BigDecimal allowanceAmount;
    private BigDecimal grossSalary;
    private BigDecimal socialInsurance;
    private BigDecimal healthInsurance;
    private BigDecimal unemploymentInsurance;
    private BigDecimal totalInsurance;
    private BigDecimal personalDeduction;
    private BigDecimal dependentDeduction;
    private BigDecimal taxableIncome;
    private BigDecimal personalIncomeTax;
    private BigDecimal netSalary;

    public String getPeriodLabel() {
        return String.format("%02d/%d", periodMonth, periodYear);
    }

    public long getPayrollId() { return payrollId; }
    public void setPayrollId(long payrollId) { this.payrollId = payrollId; }
    public long getPeriodId() { return periodId; }
    public void setPeriodId(long periodId) { this.periodId = periodId; }
    public int getPeriodYear() { return periodYear; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }
    public int getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(int periodMonth) { this.periodMonth = periodMonth; }
    public PayrollStatus getPeriodStatus() { return periodStatus; }
    public void setPeriodStatus(PayrollStatus periodStatus) { this.periodStatus = periodStatus; }
    public long getEmployeeId() { return employeeId; }
    public void setEmployeeId(long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getPositionName() { return positionName; }
    public void setPositionName(String positionName) { this.positionName = positionName; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public BigDecimal getBaseSalary() { return baseSalary; }
    public void setBaseSalary(BigDecimal baseSalary) { this.baseSalary = baseSalary; }
    public BigDecimal getAllowance() { return allowance; }
    public void setAllowance(BigDecimal allowance) { this.allowance = allowance; }
    public int getDependents() { return dependents; }
    public void setDependents(int dependents) { this.dependents = dependents; }
    public BigDecimal getStandardDays() { return standardDays; }
    public void setStandardDays(BigDecimal standardDays) { this.standardDays = standardDays; }
    public BigDecimal getWorkedDays() { return workedDays; }
    public void setWorkedDays(BigDecimal workedDays) { this.workedDays = workedDays; }
    public BigDecimal getPaidLeaveDays() { return paidLeaveDays; }
    public void setPaidLeaveDays(BigDecimal paidLeaveDays) { this.paidLeaveDays = paidLeaveDays; }
    public BigDecimal getPaidDays() { return paidDays; }
    public void setPaidDays(BigDecimal paidDays) { this.paidDays = paidDays; }
    public BigDecimal getUnpaidDays() { return unpaidDays; }
    public void setUnpaidDays(BigDecimal unpaidDays) { this.unpaidDays = unpaidDays; }
    public BigDecimal getSalaryAmount() { return salaryAmount; }
    public void setSalaryAmount(BigDecimal salaryAmount) { this.salaryAmount = salaryAmount; }
    public BigDecimal getAllowanceAmount() { return allowanceAmount; }
    public void setAllowanceAmount(BigDecimal allowanceAmount) { this.allowanceAmount = allowanceAmount; }
    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; }
    public BigDecimal getSocialInsurance() { return socialInsurance; }
    public void setSocialInsurance(BigDecimal socialInsurance) { this.socialInsurance = socialInsurance; }
    public BigDecimal getHealthInsurance() { return healthInsurance; }
    public void setHealthInsurance(BigDecimal healthInsurance) { this.healthInsurance = healthInsurance; }
    public BigDecimal getUnemploymentInsurance() { return unemploymentInsurance; }
    public void setUnemploymentInsurance(BigDecimal unemploymentInsurance) { this.unemploymentInsurance = unemploymentInsurance; }
    public BigDecimal getTotalInsurance() { return totalInsurance; }
    public void setTotalInsurance(BigDecimal totalInsurance) { this.totalInsurance = totalInsurance; }
    public BigDecimal getPersonalDeduction() { return personalDeduction; }
    public void setPersonalDeduction(BigDecimal personalDeduction) { this.personalDeduction = personalDeduction; }
    public BigDecimal getDependentDeduction() { return dependentDeduction; }
    public void setDependentDeduction(BigDecimal dependentDeduction) { this.dependentDeduction = dependentDeduction; }
    public BigDecimal getTaxableIncome() { return taxableIncome; }
    public void setTaxableIncome(BigDecimal taxableIncome) { this.taxableIncome = taxableIncome; }
    public BigDecimal getPersonalIncomeTax() { return personalIncomeTax; }
    public void setPersonalIncomeTax(BigDecimal personalIncomeTax) { this.personalIncomeTax = personalIncomeTax; }
    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }
}
