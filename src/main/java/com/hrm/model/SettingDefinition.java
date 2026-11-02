package com.hrm.model;

import java.math.BigDecimal;

/** A payroll setting row for the admin screen. */
public record SettingDefinition(String key, BigDecimal value, String description) {
}
