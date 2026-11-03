package com.hrm.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class PerformanceReview {

    private long reviewId;
    private long cycleId;
    private String cycleName;
    private CycleStatus cycleStatus;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate dueDate;
    private BigDecimal selfWeight;
    private long employeeId;
    private String employeeCode;
    private String fullName;
    private String departmentName;
    private String positionName;
    private Long reviewerId;
    private String reviewerName;
    private ReviewStatus status;
    private BigDecimal selfScore;
    private BigDecimal managerScore;
    private BigDecimal finalScore;
    private String rating;
    private String selfComment;
    private String managerComment;
    private Instant selfSubmittedAt;
    private Instant managerSubmittedAt;
    private List<ScoreLine> scores = List.of();

    public long getReviewId() { return reviewId; }
    public void setReviewId(long reviewId) { this.reviewId = reviewId; }
    public long getCycleId() { return cycleId; }
    public void setCycleId(long cycleId) { this.cycleId = cycleId; }
    public String getCycleName() { return cycleName; }
    public void setCycleName(String cycleName) { this.cycleName = cycleName; }
    public CycleStatus getCycleStatus() { return cycleStatus; }
    public void setCycleStatus(CycleStatus cycleStatus) { this.cycleStatus = cycleStatus; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public BigDecimal getSelfWeight() { return selfWeight; }
    public void setSelfWeight(BigDecimal selfWeight) { this.selfWeight = selfWeight; }
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
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    public ReviewStatus getStatus() { return status; }
    public void setStatus(ReviewStatus status) { this.status = status; }
    public BigDecimal getSelfScore() { return selfScore; }
    public void setSelfScore(BigDecimal selfScore) { this.selfScore = selfScore; }
    public BigDecimal getManagerScore() { return managerScore; }
    public void setManagerScore(BigDecimal managerScore) { this.managerScore = managerScore; }
    public BigDecimal getFinalScore() { return finalScore; }
    public void setFinalScore(BigDecimal finalScore) { this.finalScore = finalScore; }
    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }
    public String getSelfComment() { return selfComment; }
    public void setSelfComment(String selfComment) { this.selfComment = selfComment; }
    public String getManagerComment() { return managerComment; }
    public void setManagerComment(String managerComment) { this.managerComment = managerComment; }
    public Instant getSelfSubmittedAt() { return selfSubmittedAt; }
    public void setSelfSubmittedAt(Instant selfSubmittedAt) { this.selfSubmittedAt = selfSubmittedAt; }
    public Instant getManagerSubmittedAt() { return managerSubmittedAt; }
    public void setManagerSubmittedAt(Instant managerSubmittedAt) { this.managerSubmittedAt = managerSubmittedAt; }
    public List<ScoreLine> getScores() { return scores; }
    public void setScores(List<ScoreLine> scores) { this.scores = scores; }
}
