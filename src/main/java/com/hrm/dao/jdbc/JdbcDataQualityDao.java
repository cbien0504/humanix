package com.hrm.dao.jdbc;

import com.hrm.dao.DataQualityDao;
import com.hrm.model.ValidationCheck;

import java.util.List;

public class JdbcDataQualityDao implements DataQualityDao {

    private final JdbcExecutor jdbc;

    public JdbcDataQualityDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ValidationCheck> runValidation() {
        return jdbc.call("{call dbo.sp_RunDataValidation}", rs -> new ValidationCheck(rs.getString("CheckCode"),
                rs.getString("CheckName"), rs.getString("Severity"), rs.getInt("ViolationCount"),
                rs.getString("Result"), rs.getString("SampleKeys")));
    }
}
