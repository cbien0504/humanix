package com.hrm.dao;

import com.hrm.model.Employee;
import com.hrm.model.EmployeeFilter;
import com.hrm.model.EmployeeForm;
import com.hrm.model.EmployeeOption;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.model.SalaryChange;

import java.util.List;
import java.util.Optional;

public interface EmployeeDao {

    Page<Employee> search(EmployeeFilter filter, PageRequest page);

    List<Employee> findAll(EmployeeFilter filter);

    Optional<Employee> findById(long id);

    Optional<Employee> findByCode(String code);

    boolean existsByEmail(String email);

    long insert(EmployeeForm form);

    void update(long id, EmployeeForm form);

    void delete(long id);

    List<String> dependencies(long id);

    List<SalaryChange> salaryHistory(long id);

    String nextCode(String prefix);

    boolean isInTeam(long managerEmployeeId, long employeeId);

    List<Long> managerChain(long employeeId);

    List<EmployeeOption> options();
}
