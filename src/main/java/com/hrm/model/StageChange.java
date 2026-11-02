package com.hrm.model;

import java.time.Instant;

/** One pipeline transition recorded by trg_Candidates_StageHistory. */
public record StageChange(CandidateStage fromStage, CandidateStage toStage, String changedBy, Instant changedAt) {
}
