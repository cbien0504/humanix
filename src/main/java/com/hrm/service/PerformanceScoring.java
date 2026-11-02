package com.hrm.service;

import com.hrm.model.ReviewCriterion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Scoring rules BR-51/BR-52 (pure functions):
 * overall = Σ(score × weight) / Σ(weight) over the criteria, rounded to 2 decimals;
 * final = selfWeight × self + (1 − selfWeight) × manager;
 * rating A ≥ 4.5 · B ≥ 3.5 · C ≥ 2.5 · D ≥ 1.5 · E below.
 */
public final class PerformanceScoring {

    private PerformanceScoring() {
    }

    public static BigDecimal weightedAverage(Map<Long, Integer> scores, List<ReviewCriterion> criteria) {
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal weights = BigDecimal.ZERO;
        for (ReviewCriterion c : criteria) {
            Integer score = scores.get(c.getCriteriaId());
            if (score == null) {
                throw new IllegalArgumentException("Missing score for criterion " + c.getCriteriaId());
            }
            sum = sum.add(c.getWeight().multiply(BigDecimal.valueOf(score)));
            weights = weights.add(c.getWeight());
        }
        if (weights.signum() == 0) {
            throw new IllegalArgumentException("Criteria weights must not be zero");
        }
        return sum.divide(weights, 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal finalScore(BigDecimal self, BigDecimal manager, BigDecimal selfWeight) {
        return selfWeight.multiply(self).add(BigDecimal.ONE.subtract(selfWeight).multiply(manager))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static String rating(BigDecimal finalScore) {
        if (finalScore.compareTo(new BigDecimal("4.5")) >= 0) {
            return "A";
        }
        if (finalScore.compareTo(new BigDecimal("3.5")) >= 0) {
            return "B";
        }
        if (finalScore.compareTo(new BigDecimal("2.5")) >= 0) {
            return "C";
        }
        if (finalScore.compareTo(new BigDecimal("1.5")) >= 0) {
            return "D";
        }
        return "E";
    }
}
