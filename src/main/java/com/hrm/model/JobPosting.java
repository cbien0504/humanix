package com.hrm.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Job posting (table JobPostings) with candidate counters. */
public class JobPosting {

    private long jobPostingId;
    private String jobCode;
    private String title;
    private long departmentId;
    private String departmentName;
    private long positionId;
    private String positionName;
    private int openings;
    private String employmentType;
    private String location;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String description;
    private String requirements;
    private JobStatus status;
    private LocalDate openDate;
    private LocalDate closeDate;
    private String createdBy;
    private Instant createdAt;
    private int candidateCount;
    private int activeCandidateCount;
    private int hiredCount;

    public long getJobPostingId() { return jobPostingId; }
    public void setJobPostingId(long jobPostingId) { this.jobPostingId = jobPostingId; }
    public String getJobCode() { return jobCode; }
    public void setJobCode(String jobCode) { this.jobCode = jobCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public long getDepartmentId() { return departmentId; }
    public void setDepartmentId(long departmentId) { this.departmentId = departmentId; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public long getPositionId() { return positionId; }
    public void setPositionId(long positionId) { this.positionId = positionId; }
    public String getPositionName() { return positionName; }
    public void setPositionName(String positionName) { this.positionName = positionName; }
    public int getOpenings() { return openings; }
    public void setOpenings(int openings) { this.openings = openings; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public BigDecimal getSalaryMin() { return salaryMin; }
    public void setSalaryMin(BigDecimal salaryMin) { this.salaryMin = salaryMin; }
    public BigDecimal getSalaryMax() { return salaryMax; }
    public void setSalaryMax(BigDecimal salaryMax) { this.salaryMax = salaryMax; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public LocalDate getOpenDate() { return openDate; }
    public void setOpenDate(LocalDate openDate) { this.openDate = openDate; }
    public LocalDate getCloseDate() { return closeDate; }
    public void setCloseDate(LocalDate closeDate) { this.closeDate = closeDate; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public int getCandidateCount() { return candidateCount; }
    public void setCandidateCount(int candidateCount) { this.candidateCount = candidateCount; }
    public int getActiveCandidateCount() { return activeCandidateCount; }
    public void setActiveCandidateCount(int activeCandidateCount) { this.activeCandidateCount = activeCandidateCount; }
    public int getHiredCount() { return hiredCount; }
    public void setHiredCount(int hiredCount) { this.hiredCount = hiredCount; }
}
