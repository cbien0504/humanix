package com.hrm.model;

import java.time.Instant;

public record StageChange(CandidateStage fromStage, CandidateStage toStage, String changedBy, Instant changedAt) {
}
