package com.hrm.model;

import java.math.BigDecimal;
import java.time.Instant;

public class PayrollPeriod {

    private long periodId;
    private int periodYear;
    private int periodMonth;
    private BigDecimal standardWorkDays;
    private PayrollStatus status;
    private Integer employeeCount;
    private BigDecimal totalGross;
    private BigDecimal totalNet;
    private Instant calculatedAt;
    private String calculatedBy;
    private Instant approvedAt;
    private String approvedBy;
    private Instant paidAt;

    public String getLabel() {
        return String.format("%02d/%d", periodMonth, periodYear);
    }

    public long getPeriodId() { return periodId; }
    public void setPeriodId(long periodId) { this.periodId = periodId; }
    public int getPeriodYear() { return periodYear; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }
    public int getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(int periodMonth) { this.periodMonth = periodMonth; }
    public BigDecimal getStandardWorkDays() { return standardWorkDays; }
    public void setStandardWorkDays(BigDecimal standardWorkDays) { this.standardWorkDays = standardWorkDays; }
    public PayrollStatus getStatus() { return status; }
    public void setStatus(PayrollStatus status) { this.status = status; }
    public Integer getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(Integer employeeCount) { this.employeeCount = employeeCount; }
    public BigDecimal getTotalGross() { return totalGross; }
    public void setTotalGross(BigDecimal totalGross) { this.totalGross = totalGross; }
    public BigDecimal getTotalNet() { return totalNet; }
    public void setTotalNet(BigDecimal totalNet) { this.totalNet = totalNet; }
    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }
    public String getCalculatedBy() { return calculatedBy; }
    public void setCalculatedBy(String calculatedBy) { this.calculatedBy = calculatedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}
