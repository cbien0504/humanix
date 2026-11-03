package com.hrm.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeForm(String employeeCode, String fullName, String gender, LocalDate dateOfBirth,
                           String email, String phone, String address, String nationalId, String taxCode,
                           String bankAccount, String bankName, Long departmentId, Long positionId, Long managerId,
                           LocalDate hireDate, LocalDate terminationDate, EmployeeStatus status,
                           BigDecimal baseSalary, BigDecimal allowance, Integer dependents) {
}
