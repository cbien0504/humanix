package com.hrm.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Employee read model (vw_EmployeeDetails): the record plus resolved department, position,
 * manager and account information. Writes go through {@code EmployeeService.EmployeeForm}
 * so clients can never mass-assign computed or protected fields.
 */
public class Employee {

    private long employeeId;
    private String employeeCode;
    private String fullName;
    private String gender;
    private LocalDate dateOfBirth;
    private Integer age;
    private String email;
    private String phone;
    private String address;
    private String nationalId;
    private String taxCode;
    private String bankAccount;
    private String bankName;
    private Long departmentId;
    private String departmentCode;
    private String departmentName;
    private Long positionId;
    private String positionCode;
    private String positionName;
    private Long managerId;
    private String managerCode;
    private String managerName;
    private LocalDate hireDate;
    private LocalDate terminationDate;
    private BigDecimal yearsOfService;
    private EmployeeStatus status;
    private BigDecimal baseSalary;
    private BigDecimal allowance;
    private Integer dependents;
    private String username;
    private boolean hasActiveAccount;
    private Instant createdAt;
    private Instant updatedAt;

    /** Removes compensation and bank data for viewers who may not see them. */
    public Employee withoutConfidentialData() {
        baseSalary = null;
        allowance = null;
        bankAccount = null;
        bankName = null;
        taxCode = null;
        nationalId = null;
        dependents = null;
        return this;
    }

    public String getGenderLabel() {
        return gender == null ? null : switch (gender) {
            case "M" -> "Nam";
            case "F" -> "Nữ";
            default -> "Khác";
        };
    }

    public long getEmployeeId() { return employeeId; }
    public void setEmployeeId(long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }
    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public Long getPositionId() { return positionId; }
    public void setPositionId(Long positionId) { this.positionId = positionId; }
    public String getPositionCode() { return positionCode; }
    public void setPositionCode(String positionCode) { this.positionCode = positionCode; }
    public String getPositionName() { return positionName; }
    public void setPositionName(String positionName) { this.positionName = positionName; }
    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }
    public String getManagerCode() { return managerCode; }
    public void setManagerCode(String managerCode) { this.managerCode = managerCode; }
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public LocalDate getTerminationDate() { return terminationDate; }
    public void setTerminationDate(LocalDate terminationDate) { this.terminationDate = terminationDate; }
    public BigDecimal getYearsOfService() { return yearsOfService; }
    public void setYearsOfService(BigDecimal yearsOfService) { this.yearsOfService = yearsOfService; }
    public EmployeeStatus getStatus() { return status; }
    public void setStatus(EmployeeStatus status) { this.status = status; }
    public BigDecimal getBaseSalary() { return baseSalary; }
    public void setBaseSalary(BigDecimal baseSalary) { this.baseSalary = baseSalary; }
    public BigDecimal getAllowance() { return allowance; }
    public void setAllowance(BigDecimal allowance) { this.allowance = allowance; }
    public Integer getDependents() { return dependents; }
    public void setDependents(Integer dependents) { this.dependents = dependents; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public boolean isHasActiveAccount() { return hasActiveAccount; }
    public void setHasActiveAccount(boolean hasActiveAccount) { this.hasActiveAccount = hasActiveAccount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
