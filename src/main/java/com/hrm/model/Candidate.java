package com.hrm.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Candidate {

    private long candidateId;
    private long jobPostingId;
    private String jobCode;
    private String jobTitle;
    private long departmentId;
    private String fullName;
    private String email;
    private String phone;
    private String source;
    private String resumeUrl;
    private BigDecimal expectedSalary;
    private CandidateStage stage;
    private Integer rating;
    private LocalDateTime interviewAt;
    private String notes;
    private LocalDate appliedDate;
    private Long hiredEmployeeId;
    private Instant updatedAt;

    public long getCandidateId() { return candidateId; }
    public void setCandidateId(long candidateId) { this.candidateId = candidateId; }
    public long getJobPostingId() { return jobPostingId; }
    public void setJobPostingId(long jobPostingId) { this.jobPostingId = jobPostingId; }
    public String getJobCode() { return jobCode; }
    public void setJobCode(String jobCode) { this.jobCode = jobCode; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public long getDepartmentId() { return departmentId; }
    public void setDepartmentId(long departmentId) { this.departmentId = departmentId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getResumeUrl() { return resumeUrl; }
    public void setResumeUrl(String resumeUrl) { this.resumeUrl = resumeUrl; }
    public BigDecimal getExpectedSalary() { return expectedSalary; }
    public void setExpectedSalary(BigDecimal expectedSalary) { this.expectedSalary = expectedSalary; }
    public CandidateStage getStage() { return stage; }
    public void setStage(CandidateStage stage) { this.stage = stage; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public LocalDateTime getInterviewAt() { return interviewAt; }
    public void setInterviewAt(LocalDateTime interviewAt) { this.interviewAt = interviewAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDate getAppliedDate() { return appliedDate; }
    public void setAppliedDate(LocalDate appliedDate) { this.appliedDate = appliedDate; }
    public Long getHiredEmployeeId() { return hiredEmployeeId; }
    public void setHiredEmployeeId(Long hiredEmployeeId) { this.hiredEmployeeId = hiredEmployeeId; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
