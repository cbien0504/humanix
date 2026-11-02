package com.hrm.dao;

import com.hrm.model.ValidationCheck;

import java.util.List;

/** Runs the database-side data validation (sp_RunDataValidation). */
public interface DataQualityDao {

    List<ValidationCheck> runValidation();
}
