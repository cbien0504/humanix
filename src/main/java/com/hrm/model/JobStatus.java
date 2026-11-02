package com.hrm.model;

/** Job posting status (CK_JobPostings_Status). */
public enum JobStatus {
    DRAFT("Nháp"),
    OPEN("Đang tuyển"),
    ON_HOLD("Tạm dừng"),
    CLOSED("Đã đóng");

    private final String label;

    JobStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
