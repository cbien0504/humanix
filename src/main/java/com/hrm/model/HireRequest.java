package com.hrm.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HireRequest(String employeeCode, String gender, LocalDate dateOfBirth, LocalDate hireDate,
                          BigDecimal baseSalary, BigDecimal allowance, Long managerId) {
}
