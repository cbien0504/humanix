package com.hrm.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Write model of a candidate (managers may only change rating, interview time and notes). */
public record CandidateForm(Long jobPostingId, String fullName, String email, String phone, String source,
                            String resumeUrl, BigDecimal expectedSalary, Integer rating, LocalDateTime interviewAt,
                            String notes, LocalDate appliedDate) {
}
