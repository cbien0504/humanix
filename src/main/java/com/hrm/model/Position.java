package com.hrm.model;

import java.math.BigDecimal;

/** Job title with level and salary band (table Positions). */
public class Position {

    private long positionId;
    private String positionCode;
    private String positionName;
    private int jobLevel = 1;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String description;
    private boolean active = true;
    private int employeeCount;

    /** BR-03: is the salary inside this position's band? */
    public boolean accepts(BigDecimal salary) {
        return salary != null && salary.compareTo(minSalary) >= 0 && salary.compareTo(maxSalary) <= 0;
    }

    public long getPositionId() { return positionId; }
    public void setPositionId(long positionId) { this.positionId = positionId; }
    public String getPositionCode() { return positionCode; }
    public void setPositionCode(String positionCode) { this.positionCode = positionCode; }
    public String getPositionName() { return positionName; }
    public void setPositionName(String positionName) { this.positionName = positionName; }
    public int getJobLevel() { return jobLevel; }
    public void setJobLevel(int jobLevel) { this.jobLevel = jobLevel; }
    public BigDecimal getMinSalary() { return minSalary; }
    public void setMinSalary(BigDecimal minSalary) { this.minSalary = minSalary; }
    public BigDecimal getMaxSalary() { return maxSalary; }
    public void setMaxSalary(BigDecimal maxSalary) { this.maxSalary = maxSalary; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
}
