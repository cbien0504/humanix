package com.hrm.service;

import com.hrm.dao.DepartmentDao;
import com.hrm.dao.PositionDao;
import com.hrm.dao.RecruitmentDao;
import com.hrm.dao.TransactionManager;
import com.hrm.exception.BusinessException;
import com.hrm.exception.ForbiddenException;
import com.hrm.exception.NotFoundException;
import com.hrm.model.Candidate;
import com.hrm.model.CandidateForm;
import com.hrm.model.CandidateStage;
import com.hrm.model.Employee;
import com.hrm.model.EmployeeForm;
import com.hrm.model.EmployeeStatus;
import com.hrm.model.HireRequest;
import com.hrm.model.JobPosting;
import com.hrm.model.JobPostingForm;
import com.hrm.model.JobStatus;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.model.StageChange;
import com.hrm.security.AuthUser;
import com.hrm.security.Role;
import com.hrm.util.Validator;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Recruitment (UC-40 … UC-43): job postings, candidates and their pipeline.
 * HR manages everything; managers see postings of their departments and may only rate
 * candidates, plan interviews and write notes.
 */
public class RecruitmentService {

    private static final Set<String> EMPLOYMENT_TYPES = Set.of("FULL_TIME", "PART_TIME", "CONTRACT", "INTERN");
    private static final Set<String> SOURCES = Set.of("WEBSITE", "LINKEDIN", "TOPCV", "VIETNAMWORKS", "REFERRAL", "OTHER");

    private final RecruitmentDao recruitmentDao;
    private final DepartmentDao departmentDao;
    private final PositionDao positionDao;
    private final EmployeeService employeeService;
    private final AuditService audit;
    private final TransactionManager tx;
    private final Clock clock;

    public RecruitmentService(RecruitmentDao recruitmentDao, DepartmentDao departmentDao, PositionDao positionDao,
                              EmployeeService employeeService, AuditService audit, TransactionManager tx, Clock clock) {
        this.recruitmentDao = recruitmentDao;
        this.departmentDao = departmentDao;
        this.positionDao = positionDao;
        this.employeeService = employeeService;
        this.audit = audit;
        this.tx = tx;
        this.clock = clock;
    }

    /** A candidate with its pipeline history and the stages it may move to. */
    public record CandidateDetail(Candidate candidate, List<StageChange> history, Set<CandidateStage> nextStages) {
    }

    /** Pipeline board of one posting. */
    public record Pipeline(JobPosting job, Map<CandidateStage, List<Candidate>> columns) {
    }

    // ------------------------------------------------------------------ job postings

    public Page<JobPosting> searchJobs(AuthUser user, String keyword, JobStatus status, Long departmentId, PageRequest page) {
        return recruitmentDao.searchJobs(keyword, status, departmentId, scope(user), page);
    }

    public JobPosting job(AuthUser user, long id) {
        JobPosting job = recruitmentDao.findJob(id).orElseThrow(() -> NotFoundException.of("tin tuyển dụng", id));
        checkJobScope(user, id);
        return job;
    }

    public JobPosting createJob(AuthUser user, JobPostingForm form) {
        JobPostingForm f = normalize(form);
        validate(f);
        long id = recruitmentDao.insertJob(f, user.getUsername());
        audit.record("JOB_CREATED", "JobPosting", id, f.jobCode() + " – " + f.title());
        return recruitmentDao.findJob(id).orElseThrow();
    }

    public JobPosting updateJob(long id, JobPostingForm form) {
        JobPosting current = recruitmentDao.findJob(id).orElseThrow(() -> NotFoundException.of("tin tuyển dụng", id));
        JobPostingForm f = normalize(form);
        validate(f);
        if (f.openings() < current.getHiredCount()) {
            throw new BusinessException("Số lượng tuyển không được nhỏ hơn số người đã tuyển (" + current.getHiredCount() + ").");
        }
        recruitmentDao.updateJob(id, f);
        audit.record("JOB_UPDATED", "JobPosting", id, f.jobCode());
        return recruitmentDao.findJob(id).orElseThrow();
    }

    public JobPosting changeJobStatus(long id, JobStatus status) {
        JobPosting job = recruitmentDao.findJob(id).orElseThrow(() -> NotFoundException.of("tin tuyển dụng", id));
        if (status == null || status == job.getStatus()) {
            throw new BusinessException("Trạng thái mới không hợp lệ.");
        }
        if (job.getStatus() == JobStatus.CLOSED) {
            throw new BusinessException("Tin tuyển dụng đã đóng.");
        }
        LocalDate today = LocalDate.now(clock);
        recruitmentDao.updateJobStatus(id, status, status == JobStatus.OPEN ? today : null,
                status == JobStatus.CLOSED ? today : null);
        audit.record("JOB_STATUS_CHANGED", "JobPosting", id, job.getStatus() + " → " + status);
        return recruitmentDao.findJob(id).orElseThrow();
    }

    public void deleteJob(long id) {
        JobPosting job = recruitmentDao.findJob(id).orElseThrow(() -> NotFoundException.of("tin tuyển dụng", id));
        if (job.getCandidateCount() > 0) {
            throw new BusinessException("Tin đã có " + job.getCandidateCount() + " ứng viên – hãy chuyển sang \"Đã đóng\" thay vì xóa.");
        }
        recruitmentDao.deleteJob(id);
        audit.record("JOB_DELETED", "JobPosting", id, job.getJobCode());
    }

    public Pipeline pipeline(AuthUser user, long jobId) {
        JobPosting job = job(user, jobId);
        Map<CandidateStage, List<Candidate>> columns = new LinkedHashMap<>();
        for (CandidateStage s : CandidateStage.values()) {
            columns.put(s, new java.util.ArrayList<>());
        }
        for (Candidate c : recruitmentDao.candidatesOfJob(jobId)) {
            columns.get(c.getStage()).add(c);
        }
        return new Pipeline(job, columns);
    }

    // ------------------------------------------------------------------ candidates

    public Page<Candidate> searchCandidates(AuthUser user, Long jobId, CandidateStage stage, String keyword, PageRequest page) {
        return recruitmentDao.searchCandidates(jobId, stage, keyword, scope(user), page);
    }

    public CandidateDetail candidate(AuthUser user, long id) {
        Candidate c = loadCandidate(id);
        checkJobScope(user, c.getJobPostingId());
        Set<CandidateStage> next = c.getStage().next();
        next.remove(CandidateStage.HIRED);           // hiring has its own action
        return new CandidateDetail(c, recruitmentDao.history(id), next);
    }

    public Candidate addCandidate(CandidateForm form) {
        CandidateForm f = normalize(form);
        validate(f);
        JobPosting job = recruitmentDao.findJob(f.jobPostingId())
                .orElseThrow(() -> NotFoundException.of("tin tuyển dụng", f.jobPostingId()));
        if (job.getStatus() != JobStatus.OPEN) {
            throw new BusinessException("Chỉ nhận hồ sơ cho tin đang tuyển (trạng thái hiện tại: " + job.getStatus().getLabel() + ").");
        }
        long id = recruitmentDao.insertCandidate(f);    // UQ_Candidates_JobEmail → "đã nộp hồ sơ"
        audit.record("CANDIDATE_ADDED", "Candidate", id, f.fullName() + " → " + job.getJobCode());
        return loadCandidate(id);
    }

    /** HR edits everything; a manager in scope only rating, interview time and notes. */
    public Candidate updateCandidate(AuthUser user, long id, CandidateForm form) {
        Candidate current = loadCandidate(id);
        CandidateForm f = normalize(form);
        if (user.isHrOrAdmin()) {
            validate(new CandidateForm(current.getJobPostingId(), f.fullName(), f.email(), f.phone(), f.source(), f.resumeUrl(),
                    f.expectedSalary(), f.rating(), f.interviewAt(), f.notes(), current.getAppliedDate()));
            recruitmentDao.updateCandidate(id, f);
        } else {
            checkJobScope(user, current.getJobPostingId());
            new Validator().range("rating", f.rating(), 1, 5, "Điểm đánh giá từ 1 đến 5.")
                    .maxLength("notes", f.notes(), 2000).throwIfInvalid();
            recruitmentDao.updateCandidateReview(id, f.rating(), f.interviewAt(), f.notes());
        }
        audit.record("CANDIDATE_UPDATED", "Candidate", id, null);
        return loadCandidate(id);
    }

    public CandidateDetail moveStage(AuthUser user, long id, CandidateStage target) {
        Candidate c = loadCandidate(id);
        if (target == CandidateStage.HIRED) {
            throw new BusinessException("Dùng chức năng \"Tiếp nhận\" để chuyển ứng viên sang Đã tuyển.");
        }
        if (target == null || !c.getStage().canMoveTo(target)) {
            throw new BusinessException("Không thể chuyển từ \"" + c.getStage().getLabel() + "\" sang \""
                    + (target == null ? "?" : target.getLabel()) + "\".");
        }
        if (recruitmentDao.changeStage(id, c.getStage(), target, null) == 0) {
            throw new BusinessException("Hồ sơ vừa được người khác cập nhật. Vui lòng tải lại.");
        }
        audit.record("CANDIDATE_STAGE_CHANGED", "Candidate", id, c.getStage() + " → " + target);
        return candidate(user, id);
    }

    public void deleteCandidate(long id) {
        Candidate c = loadCandidate(id);
        if (c.getStage() == CandidateStage.HIRED) {
            throw new BusinessException("Không thể xóa ứng viên đã được tiếp nhận.");
        }
        recruitmentDao.deleteCandidate(id);
        audit.record("CANDIDATE_DELETED", "Candidate", id, c.getFullName());
    }

    /**
     * UC-43 / BR-43: creates a probationary employee from a candidate in OFFER, marks the
     * candidate HIRED and closes the posting when all openings are filled – atomically.
     */
    public Employee hire(long candidateId, HireRequest req) {
        Candidate c = loadCandidate(candidateId);
        if (c.getStage() != CandidateStage.OFFER) {
            throw new BusinessException("Chỉ tiếp nhận được ứng viên ở bước \"Đề nghị nhận việc\".");
        }
        JobPosting job = recruitmentDao.findJob(c.getJobPostingId()).orElseThrow();
        Long managerId = req.managerId() != null ? req.managerId()
                : departmentDao.findById(job.getDepartmentId()).map(d -> d.getManagerId()).orElse(null);
        String code = Validator.clean(req.employeeCode()) != null ? req.employeeCode() : employeeService.nextCode();
        EmployeeForm form = new EmployeeForm(code, c.getFullName(), req.gender(), req.dateOfBirth(), c.getEmail(), c.getPhone(),
                null, null, null, null, null, job.getDepartmentId(), job.getPositionId(), managerId,
                req.hireDate() == null ? LocalDate.now(clock) : req.hireDate(), null, EmployeeStatus.PROBATION,
                req.baseSalary() != null ? req.baseSalary() : c.getExpectedSalary(), req.allowance(), 0);
        return tx.inTransaction(() -> {
            Employee employee = employeeService.create(form);
            if (recruitmentDao.changeStage(candidateId, CandidateStage.OFFER, CandidateStage.HIRED, employee.getEmployeeId()) == 0) {
                throw new BusinessException("Hồ sơ vừa được người khác cập nhật. Vui lòng tải lại.");
            }
            if (job.getHiredCount() + 1 >= job.getOpenings() && job.getStatus() != JobStatus.CLOSED) {
                recruitmentDao.updateJobStatus(job.getJobPostingId(), JobStatus.CLOSED, null, LocalDate.now(clock));
            }
            audit.record("CANDIDATE_HIRED", "Candidate", candidateId, c.getFullName() + " → " + employee.getEmployeeCode());
            return employee;
        });
    }

    public Map<String, Long> stats() {
        return recruitmentDao.recruitmentStats();
    }

    // ------------------------------------------------------------------ helpers

    private Long scope(AuthUser user) {
        if (user.isHrOrAdmin()) {
            return null;
        }
        if (user.hasRole(Role.MANAGER) && user.getEmployeeId() != null) {
            return user.getEmployeeId();
        }
        throw new ForbiddenException();
    }

    private void checkJobScope(AuthUser user, long jobId) {
        Long managerId = scope(user);
        if (managerId != null && !recruitmentDao.isJobInManagerScope(jobId, managerId)) {
            throw new ForbiddenException("Tin tuyển dụng không thuộc phòng ban bạn quản lý.");
        }
    }

    private Candidate loadCandidate(long id) {
        return recruitmentDao.findCandidate(id).orElseThrow(() -> NotFoundException.of("ứng viên", id));
    }

    private static JobPostingForm normalize(JobPostingForm f) {
        return new JobPostingForm(Validator.upper(f.jobCode()), Validator.clean(f.title()), f.departmentId(), f.positionId(),
                f.openings() == null ? 1 : f.openings(), f.employmentType() == null ? "FULL_TIME" : f.employmentType().toUpperCase(),
                Validator.clean(f.location()), f.salaryMin(), f.salaryMax(), Validator.clean(f.description()),
                Validator.clean(f.requirements()), f.status() == null ? JobStatus.DRAFT : f.status(), f.openDate(), f.closeDate());
    }

    private void validate(JobPostingForm f) {
        Validator v = new Validator()
                .required("jobCode", f.jobCode(), "Vui lòng nhập mã tin.")
                .pattern("jobCode", f.jobCode(), Validator.CODE, "Mã gồm 3–20 ký tự: chữ in hoa, số, dấu gạch ngang.")
                .required("title", f.title(), "Vui lòng nhập tiêu đề.")
                .maxLength("title", f.title(), 200)
                .required("departmentId", f.departmentId(), "Vui lòng chọn phòng ban.")
                .required("positionId", f.positionId(), "Vui lòng chọn chức danh.")
                .range("openings", f.openings(), 1, 500, "Số lượng tuyển từ 1 đến 500.")
                .check(EMPLOYMENT_TYPES.contains(f.employmentType()), "employmentType", "Hình thức làm việc không hợp lệ.")
                .nonNegative("salaryMin", f.salaryMin(), "Không được âm.")
                .nonNegative("salaryMax", f.salaryMax(), "Không được âm.")
                .check(f.salaryMin() == null || f.salaryMax() == null || f.salaryMax().compareTo(f.salaryMin()) >= 0,
                        "salaryMax", "Lương tối đa phải ≥ lương tối thiểu.")
                .notBefore("closeDate", f.closeDate(), f.openDate(), "Ngày đóng phải sau ngày mở.");
        if (f.departmentId() != null) {
            v.check(departmentDao.findById(f.departmentId()).isPresent(), "departmentId", "Phòng ban không tồn tại.");
        }
        if (f.positionId() != null) {
            v.check(positionDao.findById(f.positionId()).isPresent(), "positionId", "Chức danh không tồn tại.");
        }
        v.throwIfInvalid();
    }

    private CandidateForm normalize(CandidateForm f) {
        return new CandidateForm(f.jobPostingId(), Validator.clean(f.fullName()), Validator.lower(f.email()),
                Validator.clean(f.phone()), f.source() == null ? "WEBSITE" : f.source().toUpperCase(),
                Validator.clean(f.resumeUrl()), f.expectedSalary(), f.rating(), f.interviewAt(), Validator.clean(f.notes()),
                f.appliedDate() == null ? LocalDate.now(clock) : f.appliedDate());
    }

    private static void validate(CandidateForm f) {
        new Validator()
                .required("jobPostingId", f.jobPostingId(), "Vui lòng chọn tin tuyển dụng.")
                .required("fullName", f.fullName(), "Vui lòng nhập họ tên.")
                .maxLength("fullName", f.fullName(), 150)
                .required("email", f.email(), "Vui lòng nhập email.")
                .email("email", f.email())
                .phone("phone", f.phone())
                .check(SOURCES.contains(f.source()), "source", "Nguồn ứng tuyển không hợp lệ.")
                .check(f.resumeUrl() == null || f.resumeUrl().matches("^https?://.+"), "resumeUrl",
                        "Link CV phải bắt đầu bằng http:// hoặc https://.")
                .nonNegative("expectedSalary", f.expectedSalary(), "Không được âm.")
                .range("rating", f.rating(), 1, 5, "Điểm đánh giá từ 1 đến 5.")
                .maxLength("notes", f.notes(), 2000)
                .throwIfInvalid();
    }
}
