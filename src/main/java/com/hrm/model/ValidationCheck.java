package com.hrm.model;

public record ValidationCheck(String checkCode, String checkName, String severity, int violationCount,
                              String result, String sampleKeys) {
}
