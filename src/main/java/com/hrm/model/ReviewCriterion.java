package com.hrm.model;

import java.math.BigDecimal;

public class ReviewCriterion {

    private final long criteriaId;
    private final String name;
    private final String description;
    private final BigDecimal weight;

    public ReviewCriterion(long criteriaId, String name, String description, BigDecimal weight) {
        this.criteriaId = criteriaId;
        this.name = name;
        this.description = description;
        this.weight = weight;
    }

    public long getCriteriaId() { return criteriaId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getWeight() { return weight; }
}
