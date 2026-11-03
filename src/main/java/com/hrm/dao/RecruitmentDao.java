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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RecruitmentDao {

    Page<JobPosting> searchJobs(String keyword, JobStatus status, Long departmentId, Long scopeManagerId, PageRequest page);

    Optional<JobPosting> findJob(long jobPostingId);

    long insertJob(JobPostingForm form, String createdBy);

    void updateJob(long jobPostingId, JobPostingForm form);

    void updateJobStatus(long jobPostingId, JobStatus status, LocalDate openDateIfMissing, LocalDate closeDate);

    void deleteJob(long jobPostingId);

    boolean isJobInManagerScope(long jobPostingId, long managerEmployeeId);

    List<Candidate> candidatesOfJob(long jobPostingId);

    Page<Candidate> searchCandidates(Long jobPostingId, CandidateStage stage, String keyword, Long scopeManagerId,
                                     PageRequest page);

    Optional<Candidate> findCandidate(long candidateId);

    long insertCandidate(CandidateForm form);

    void updateCandidate(long candidateId, CandidateForm form);

    void updateCandidateReview(long candidateId, Integer rating, LocalDateTime interviewAt, String notes);

    void deleteCandidate(long candidateId);

    int changeStage(long candidateId, CandidateStage expected, CandidateStage target, Long hiredEmployeeId);

    List<StageChange> history(long candidateId);

    Map<String, Long> recruitmentStats();
}
