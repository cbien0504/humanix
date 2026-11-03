package com.hrm.model;

import java.math.BigDecimal;

public record PositionForm(String positionCode, String positionName, Integer jobLevel, BigDecimal minSalary,
                           BigDecimal maxSalary, String description, Boolean active) {
}
