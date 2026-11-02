package com.hrm.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Write model of a job posting. */
public record JobPostingForm(String jobCode, String title, Long departmentId, Long positionId, Integer openings,
                             String employmentType, String location, BigDecimal salaryMin, BigDecimal salaryMax,
                             String description, String requirements, JobStatus status, LocalDate openDate,
                             LocalDate closeDate) {
}
