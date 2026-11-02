package com.hrm.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Review cycle (table ReviewCycles) with its progress counters. */
public class ReviewCycle {

    private long cycleId;
    private String cycleName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate dueDate;
    private BigDecimal selfWeight;
    private CycleStatus status;
    private String createdBy;
    private int totalReviews;
    private int pendingSelf;
    private int pendingManager;
    private int completed;
    private BigDecimal averageScore;

    public long getCycleId() { return cycleId; }
    public void setCycleId(long cycleId) { this.cycleId = cycleId; }
    public String getCycleName() { return cycleName; }
    public void setCycleName(String cycleName) { this.cycleName = cycleName; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public BigDecimal getSelfWeight() { return selfWeight; }
    public void setSelfWeight(BigDecimal selfWeight) { this.selfWeight = selfWeight; }
    public CycleStatus getStatus() { return status; }
    public void setStatus(CycleStatus status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }
    public int getPendingSelf() { return pendingSelf; }
    public void setPendingSelf(int pendingSelf) { this.pendingSelf = pendingSelf; }
    public int getPendingManager() { return pendingManager; }
    public void setPendingManager(int pendingManager) { this.pendingManager = pendingManager; }
    public int getCompleted() { return completed; }
    public void setCompleted(int completed) { this.completed = completed; }
    public BigDecimal getAverageScore() { return averageScore; }
    public void setAverageScore(BigDecimal averageScore) { this.averageScore = averageScore; }
}
