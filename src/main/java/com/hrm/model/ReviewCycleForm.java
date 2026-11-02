package com.hrm.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Write model of a review cycle. */
public record ReviewCycleForm(String cycleName, LocalDate periodStart, LocalDate periodEnd, LocalDate dueDate,
                              BigDecimal selfWeight) {
}
