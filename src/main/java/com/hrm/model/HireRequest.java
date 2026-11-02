package com.hrm.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Data needed to turn a candidate in OFFER into a probationary employee (UC-43). */
public record HireRequest(String employeeCode, String gender, LocalDate dateOfBirth, LocalDate hireDate,
                          BigDecimal baseSalary, BigDecimal allowance, Long managerId) {
}
