package com.hrm.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Write model for creating/updating an employee (JSON body, CSV import row). Only these
 * fields can be set by clients – protects against mass assignment.
 */
public record EmployeeForm(String employeeCode, String fullName, String gender, LocalDate dateOfBirth,
                           String email, String phone, String address, String nationalId, String taxCode,
                           String bankAccount, String bankName, Long departmentId, Long positionId, Long managerId,
                           LocalDate hireDate, LocalDate terminationDate, EmployeeStatus status,
                           BigDecimal baseSalary, BigDecimal allowance, Integer dependents) {
}
