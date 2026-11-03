package com.hrm.model;

import java.math.BigDecimal;

public record SettingDefinition(String key, BigDecimal value, String description) {
}
