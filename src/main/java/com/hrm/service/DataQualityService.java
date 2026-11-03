package com.hrm.service;

import com.hrm.dao.DataQualityDao;
import com.hrm.model.ValidationCheck;

import java.util.List;

public class DataQualityService {

    private final DataQualityDao dao;
    private final AuditService audit;

    public DataQualityService(DataQualityDao dao, AuditService audit) {
        this.dao = dao;
        this.audit = audit;
    }

    public List<ValidationCheck> run() {
        List<ValidationCheck> checks = dao.runValidation();
        long failed = checks.stream().filter(c -> "FAIL".equals(c.result())).count();
        audit.record("DATA_VALIDATION_RUN", "Database", null, failed + " kiểm tra lỗi / " + checks.size());
        return checks;
    }
}
