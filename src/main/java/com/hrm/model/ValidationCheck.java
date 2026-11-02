package com.hrm.model;

/** Result row of sp_RunDataValidation. */
public record ValidationCheck(String checkCode, String checkName, String severity, int violationCount,
                              String result, String sampleKeys) {
}
