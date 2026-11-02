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

    Optional<Employee> findById(long id);

    Optional<Employee> findByCode(String code);

    boolean existsByEmail(String email);

    long insert(EmployeeForm form);

    void update(long id, EmployeeForm form);

    /** Deletes the employee, its unused leave balances and (by cascade) its login. */
    void delete(long id);

    /** Labels of business data that reference the employee (empty = safe to delete). */
    List<String> dependencies(long id);

    List<SalaryChange> salaryHistory(long id);

    /** Next free code with the given prefix, e.g. EMP0037. */
    String nextCode(String prefix);

    /** True when {@code employeeId} reports to the manager directly or works in a department he/she manages. */
    boolean isInTeam(long managerEmployeeId, long employeeId);

    /** All managers above the employee (direct manager first). */
    List<Long> managerChain(long employeeId);

    /** Employees not terminated, for drop-downs. */
    List<EmployeeOption> options();
}
