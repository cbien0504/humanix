package com.hrm.model;

import java.util.EnumSet;
import java.util.Set;

public enum CandidateStage {
    APPLIED("Mới nộp"),
    SCREENING("Sàng lọc"),
    INTERVIEW("Phỏng vấn"),
    OFFER("Đề nghị nhận việc"),
    HIRED("Đã tuyển"),
    REJECTED("Từ chối"),
    WITHDRAWN("Rút hồ sơ");

    private final String label;

    CandidateStage(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isFinal() {
        return this == HIRED || this == REJECTED || this == WITHDRAWN;
    }

    public Set<CandidateStage> next() {
        return switch (this) {
            case APPLIED -> EnumSet.of(SCREENING, REJECTED, WITHDRAWN);
            case SCREENING -> EnumSet.of(INTERVIEW, REJECTED, WITHDRAWN);
            case INTERVIEW -> EnumSet.of(OFFER, REJECTED, WITHDRAWN);
            case OFFER -> EnumSet.of(HIRED, REJECTED, WITHDRAWN);
            default -> EnumSet.noneOf(CandidateStage.class);
        };
    }

    public boolean canMoveTo(CandidateStage target) {
        return next().contains(target);
    }
}
