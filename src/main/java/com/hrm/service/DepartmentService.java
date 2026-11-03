package com.hrm.service;

import com.hrm.dao.DepartmentDao;
import com.hrm.dao.EmployeeDao;
import com.hrm.exception.BusinessException;
import com.hrm.exception.NotFoundException;
import com.hrm.model.Department;
import com.hrm.model.DepartmentForm;
import com.hrm.util.Validator;

import java.util.List;
import java.util.regex.Pattern;

public class DepartmentService {

    static final Pattern CODE = Pattern.compile("^[A-Z0-9_-]{2,20}$");

    private final DepartmentDao departmentDao;
    private final EmployeeDao employeeDao;
    private final AuditService audit;

    public DepartmentService(DepartmentDao departmentDao, EmployeeDao employeeDao, AuditService audit) {
        this.departmentDao = departmentDao;
        this.employeeDao = employeeDao;
        this.audit = audit;
    }

    public List<Department> list() {
        return departmentDao.findAll();
    }

    public Department get(long id) {
        return departmentDao.findById(id).orElseThrow(() -> NotFoundException.of("phòng ban", id));
    }

    public Department create(DepartmentForm form) {
        DepartmentForm f = normalize(form);
        validate(f);
        long id = departmentDao.insert(f);
        audit.record("DEPARTMENT_CREATED", "Department", id, f.departmentCode());
        return get(id);
    }

    public Department update(long id, DepartmentForm form) {
        Department current = get(id);
        DepartmentForm f = normalize(form);
        validate(f);
        if (Boolean.FALSE.equals(f.active()) && current.getEmployeeCount() > 0) {
            throw new BusinessException("Phòng ban đang có " + current.getEmployeeCount()
                    + " nhân viên, hãy chuyển nhân viên trước khi ngừng hoạt động.");
        }
        departmentDao.update(id, f);
        audit.record("DEPARTMENT_UPDATED", "Department", id, f.departmentCode());
        return get(id);
    }

    public void delete(long id) {
        Department d = get(id);
        if (d.getEmployeeCount() > 0) {
            throw new BusinessException("Phòng ban đang có " + d.getEmployeeCount() + " nhân viên nên không thể xóa.");
        }
        departmentDao.delete(id);
        audit.record("DEPARTMENT_DELETED", "Department", id, d.getDepartmentCode());
    }

    private static DepartmentForm normalize(DepartmentForm f) {
        return new DepartmentForm(Validator.upper(f.departmentCode()), Validator.clean(f.departmentName()),
                Validator.clean(f.description()), f.managerId(), f.active() == null || f.active());
    }

    private void validate(DepartmentForm f) {
        new Validator()
                .required("departmentCode", f.departmentCode(), "Vui lòng nhập mã phòng ban.")
                .pattern("departmentCode", f.departmentCode(), CODE, "Mã 2–20 ký tự: chữ in hoa, số, '_' hoặc '-'.")
                .required("departmentName", f.departmentName(), "Vui lòng nhập tên phòng ban.")
                .maxLength("departmentName", f.departmentName(), 150)
                .maxLength("description", f.description(), 500)
                .check(f.managerId() == null || employeeDao.findById(f.managerId()).isPresent(), "managerId",
                        "Trưởng phòng không tồn tại.")
                .throwIfInvalid();
    }
}
