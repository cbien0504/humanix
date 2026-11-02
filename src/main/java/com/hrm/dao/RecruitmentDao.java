package com.hrm.dao;

import com.hrm.model.Candidate;
import com.hrm.model.CandidateForm;
import com.hrm.model.CandidateStage;
import com.hrm.model.JobPosting;
import com.hrm.model.JobPostingForm;
import com.hrm.model.JobStatus;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.model.StageChange;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RecruitmentDao {

    /** {@code scopeManagerId}: only postings of departments the manager runs or belongs to. */
    Page<JobPosting> searchJobs(String keyword, JobStatus status, Long departmentId, Long scopeManagerId, PageRequest page);

    Optional<JobPosting> findJob(long jobPostingId);

    long insertJob(JobPostingForm form, String createdBy);

    void updateJob(long jobPostingId, JobPostingForm form);

    /** Changes the status; {@code openDateIfMissing}/{@code closeDate} are applied when not null. */
    void updateJobStatus(long jobPostingId, JobStatus status, java.time.LocalDate openDateIfMissing, java.time.LocalDate closeDate);

    void deleteJob(long jobPostingId);

    boolean isJobInManagerScope(long jobPostingId, long managerEmployeeId);

    List<Candidate> candidatesOfJob(long jobPostingId);

    Page<Candidate> searchCandidates(Long jobPostingId, CandidateStage stage, String keyword, Long scopeManagerId,
                                     PageRequest page);

    Optional<Candidate> findCandidate(long candidateId);

    long insertCandidate(CandidateForm form);

    void updateCandidate(long candidateId, CandidateForm form);

    void updateCandidateReview(long candidateId, Integer rating, java.time.LocalDateTime interviewAt, String notes);

    void deleteCandidate(long candidateId);

    /** Optimistic stage change ({@code WHERE Stage = expected}); returns updated rows. */
    int changeStage(long candidateId, CandidateStage expected, CandidateStage target, Long hiredEmployeeId);

    List<StageChange> history(long candidateId);

    Map<String, Long> recruitmentStats();
}
