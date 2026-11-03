package com.hrm.model;

import java.math.BigDecimal;

public class ScoreLine {

    private final long criteriaId;
    private final String name;
    private final String description;
    private final BigDecimal weight;
    private final Integer selfScore;
    private final Integer managerScore;

    public ScoreLine(long criteriaId, String name, String description, BigDecimal weight, Integer selfScore, Integer managerScore) {
        this.criteriaId = criteriaId;
        this.name = name;
        this.description = description;
        this.weight = weight;
        this.selfScore = selfScore;
        this.managerScore = managerScore;
    }

    public long getCriteriaId() { return criteriaId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getWeight() { return weight; }
    public Integer getSelfScore() { return selfScore; }
    public Integer getManagerScore() { return managerScore; }
}
