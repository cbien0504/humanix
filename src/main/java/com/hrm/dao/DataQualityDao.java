package com.hrm.dao;

import com.hrm.model.ValidationCheck;

import java.util.List;

public interface DataQualityDao {

    List<ValidationCheck> runValidation();
}
