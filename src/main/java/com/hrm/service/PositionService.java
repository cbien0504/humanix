package com.hrm.service;

import com.hrm.dao.PositionDao;
import com.hrm.exception.BusinessException;
import com.hrm.exception.NotFoundException;
import com.hrm.model.Position;
import com.hrm.model.PositionForm;
import com.hrm.util.Validator;

import java.math.BigDecimal;
import java.util.List;

/** Position (job title & salary band) management (UC-11). */
public class PositionService {

    private final PositionDao positionDao;
    private final AuditService audit;

    public PositionService(PositionDao positionDao, AuditService audit) {
        this.positionDao = positionDao;
        this.audit = audit;
    }

    public List<Position> list() {
        return positionDao.findAll();
    }

    public Position get(long id) {
        return positionDao.findById(id).orElseThrow(() -> NotFoundException.of("chức danh", id));
    }

    public Position create(PositionForm form) {
        PositionForm f = normalize(form);
        validate(f);
        long id = positionDao.insert(f);
        audit.record("POSITION_CREATED", "Position", id, f.positionCode());
        return get(id);
    }

    public Position update(long id, PositionForm form) {
        get(id);
        PositionForm f = normalize(form);
        validate(f);
        positionDao.update(id, f);
        audit.record("POSITION_UPDATED", "Position", id, f.positionCode() + " " + f.minSalary() + "-" + f.maxSalary());
        return get(id);
    }

    public void delete(long id) {
        Position p = get(id);
        if (p.getEmployeeCount() > 0) {
            throw new BusinessException("Chức danh đang được gán cho " + p.getEmployeeCount() + " nhân viên nên không thể xóa.");
        }
        positionDao.delete(id);
        audit.record("POSITION_DELETED", "Position", id, p.getPositionCode());
    }

    private static PositionForm normalize(PositionForm f) {
        return new PositionForm(Validator.upper(f.positionCode()), Validator.clean(f.positionName()),
                f.jobLevel() == null ? 1 : f.jobLevel(), f.minSalary() == null ? BigDecimal.ZERO : f.minSalary(),
                f.maxSalary(), Validator.clean(f.description()), f.active() == null || f.active());
    }

    private static void validate(PositionForm f) {
        new Validator()
                .required("positionCode", f.positionCode(), "Vui lòng nhập mã chức danh.")
                .pattern("positionCode", f.positionCode(), DepartmentService.CODE, "Mã 2–20 ký tự: chữ in hoa, số, '_' hoặc '-'.")
                .required("positionName", f.positionName(), "Vui lòng nhập tên chức danh.")
                .maxLength("positionName", f.positionName(), 150)
                .range("jobLevel", f.jobLevel(), 1, 10, "Cấp bậc từ 1 đến 10.")
                .nonNegative("minSalary", f.minSalary(), "Lương tối thiểu không được âm.")
                .required("maxSalary", f.maxSalary(), "Vui lòng nhập lương tối đa.")
                .check(f.maxSalary() == null || f.minSalary() == null || f.maxSalary().compareTo(f.minSalary()) >= 0,
                        "maxSalary", "Lương tối đa phải lớn hơn hoặc bằng lương tối thiểu.")
                .throwIfInvalid();
    }
}
