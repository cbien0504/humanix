package com.hrm.model;

import java.util.Map;

public record ScoreSubmission(Map<Long, Integer> scores, String comment) {
}
