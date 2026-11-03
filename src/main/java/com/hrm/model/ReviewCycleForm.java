package com.hrm.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReviewCycleForm(String cycleName, LocalDate periodStart, LocalDate periodEnd, LocalDate dueDate,
                              BigDecimal selfWeight) {
}
