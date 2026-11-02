package com.hrm.model;

import java.util.Map;

/** Scores per criterion (criteriaId → 1..5) and a comment, submitted by employee or manager. */
public record ScoreSubmission(Map<Long, Integer> scores, String comment) {
}
