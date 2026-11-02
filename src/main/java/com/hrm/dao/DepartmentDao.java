package com.hrm.dao;

import com.hrm.model.Department;
import com.hrm.model.DepartmentForm;

import java.util.List;
import java.util.Optional;

public interface DepartmentDao {

    List<Department> findAll();

    Optional<Department> findById(long id);

    Optional<Department> findByCode(String code);

    long insert(DepartmentForm form);

    void update(long id, DepartmentForm form);

    void delete(long id);
}
