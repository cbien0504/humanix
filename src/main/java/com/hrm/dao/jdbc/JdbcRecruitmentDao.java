package com.hrm.dao.jdbc;

import com.hrm.dao.RecruitmentDao;
import com.hrm.model.Candidate;
import com.hrm.model.CandidateForm;
import com.hrm.model.CandidateStage;
import com.hrm.model.JobPosting;
import com.hrm.model.JobPostingForm;
import com.hrm.model.JobStatus;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.model.StageChange;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcRecruitmentDao implements RecruitmentDao {

    private static final String JOB_SELECT = """
            SELECT jp.JobPostingId, jp.JobCode, jp.Title, jp.DepartmentId, d.DepartmentName, jp.PositionId, p.PositionName,
                   jp.Openings, jp.EmploymentType, jp.Location, jp.SalaryMin, jp.SalaryMax, jp.Description, jp.Requirements,
                   jp.Status, jp.OpenDate, jp.CloseDate, jp.CreatedBy, jp.CreatedAt,
                   c.CandidateCount, c.ActiveCount, c.HiredCount
            FROM dbo.JobPostings AS jp
            JOIN dbo.Departments AS d ON d.DepartmentId = jp.DepartmentId
            JOIN dbo.Positions AS p ON p.PositionId = jp.PositionId
            OUTER APPLY (SELECT COUNT(*) AS CandidateCount,
                                SUM(CASE WHEN ca.Stage NOT IN ('HIRED', 'REJECTED', 'WITHDRAWN') THEN 1 ELSE 0 END) AS ActiveCount,
                                SUM(CASE WHEN ca.Stage = 'HIRED' THEN 1 ELSE 0 END) AS HiredCount
                         FROM dbo.Candidates AS ca WHERE ca.JobPostingId = jp.JobPostingId) AS c
            """;

    private static final String CANDIDATE_SELECT = """
            SELECT ca.CandidateId, ca.JobPostingId, jp.JobCode, jp.Title AS JobTitle, jp.DepartmentId, ca.FullName, ca.Email,
                   ca.Phone, ca.Source, ca.ResumeUrl, ca.ExpectedSalary, ca.Stage, ca.Rating, ca.InterviewAt, ca.Notes,
                   ca.AppliedDate, ca.HiredEmployeeId, ca.UpdatedAt
            FROM dbo.Candidates AS ca
            JOIN dbo.JobPostings AS jp ON jp.JobPostingId = ca.JobPostingId
            """;

    /** Departments a manager runs or works in. */
    private static final String MANAGER_SCOPE = "(%s IN (SELECT DepartmentId FROM dbo.Departments WHERE ManagerId = ?) "
            + "OR %s = (SELECT DepartmentId FROM dbo.Employees WHERE EmployeeId = ?))";

    private static final Map<String, String> JOB_SORT = Map.of(
            "code", "jp.JobCode", "title", "jp.Title", "status", "jp.Status", "openDate", "jp.OpenDate",
            "department", "d.DepartmentName");
    private static final Map<String, String> CANDIDATE_SORT = Map.of(
            "name", "ca.FullName", "applied", "ca.AppliedDate", "stage", "ca.Stage", "rating", "ca.Rating");

    private final JdbcExecutor jdbc;

    public JdbcRecruitmentDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    static JobPosting mapJob(ResultSet rs) throws SQLException {
        JobPosting j = new JobPosting();
        j.setJobPostingId(rs.getLong("JobPostingId"));
        j.setJobCode(rs.getString("JobCode"));
        j.setTitle(rs.getString("Title"));
        j.setDepartmentId(rs.getLong("DepartmentId"));
        j.setDepartmentName(rs.getString("DepartmentName"));
        j.setPositionId(rs.getLong("PositionId"));
        j.setPositionName(rs.getString("PositionName"));
        j.setOpenings(rs.getInt("Openings"));
        j.setEmploymentType(rs.getString("EmploymentType"));
        j.setLocation(rs.getString("Location"));
        j.setSalaryMin(rs.getBigDecimal("SalaryMin"));
        j.setSalaryMax(rs.getBigDecimal("SalaryMax"));
        j.setDescription(rs.getString("Description"));
        j.setRequirements(rs.getString("Requirements"));
        j.setStatus(JobStatus.valueOf(rs.getString("Status")));
        j.setOpenDate(Rows.date(rs, "OpenDate"));
        j.setCloseDate(Rows.date(rs, "CloseDate"));
        j.setCreatedBy(rs.getString("CreatedBy"));
        j.setCreatedAt(Rows.instant(rs, "CreatedAt"));
        j.setCandidateCount(rs.getInt("CandidateCount"));
        j.setActiveCandidateCount(rs.getInt("ActiveCount"));
        j.setHiredCount(rs.getInt("HiredCount"));
        return j;
    }

    static Candidate mapCandidate(ResultSet rs) throws SQLException {
        Candidate c = new Candidate();
        c.setCandidateId(rs.getLong("CandidateId"));
        c.setJobPostingId(rs.getLong("JobPostingId"));
        c.setJobCode(rs.getString("JobCode"));
        c.setJobTitle(rs.getString("JobTitle"));
        c.setDepartmentId(rs.getLong("DepartmentId"));
        c.setFullName(rs.getString("FullName"));
        c.setEmail(rs.getString("Email"));
        c.setPhone(rs.getString("Phone"));
        c.setSource(rs.getString("Source"));
        c.setResumeUrl(rs.getString("ResumeUrl"));
        c.setExpectedSalary(rs.getBigDecimal("ExpectedSalary"));
        c.setStage(CandidateStage.valueOf(rs.getString("Stage")));
        c.setRating(Rows.integer(rs, "Rating"));
        c.setInterviewAt(Rows.dateTime(rs, "InterviewAt"));
        c.setNotes(rs.getString("Notes"));
        c.setAppliedDate(Rows.date(rs, "AppliedDate"));
        c.setHiredEmployeeId(Rows.longValue(rs, "HiredEmployeeId"));
        c.setUpdatedAt(Rows.instant(rs, "UpdatedAt"));
        return c;
    }

    @Override
    public Page<JobPosting> searchJobs(String keyword, JobStatus status, Long departmentId, Long scopeManagerId, PageRequest page) {
        SqlWhere where = new SqlWhere()
                .andContains(keyword, "jp.JobCode", "jp.Title")
                .andIfPresent("jp.Status = ?", status == null ? null : status.name())
                .andIfPresent("jp.DepartmentId = ?", departmentId);
        if (scopeManagerId != null) {
            where.and(String.format(MANAGER_SCOPE, "jp.DepartmentId", "jp.DepartmentId"), scopeManagerId, scopeManagerId);
        }
        List<JobPosting> items = jdbc.query(JOB_SELECT + where.toSql() + SqlOrder.of(JOB_SORT, page, "jp.CreatedAt", "jp.JobPostingId")
                + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", JdbcRecruitmentDao::mapJob, where.paramsWith(page.offset(), page.size()));
        long total = jdbc.count("SELECT COUNT(*) FROM dbo.JobPostings AS jp JOIN dbo.Departments AS d ON d.DepartmentId = jp.DepartmentId"
                + where.toSql(), where.params().toArray());
        return Page.of(items, page, total);
    }

    @Override
    public Optional<JobPosting> findJob(long jobPostingId) {
        return jdbc.queryOne(JOB_SELECT + " WHERE jp.JobPostingId = ?", JdbcRecruitmentDao::mapJob, jobPostingId);
    }

    @Override
    public long insertJob(JobPostingForm f, String createdBy) {
        return jdbc.insert("""
                INSERT INTO dbo.JobPostings (JobCode, Title, DepartmentId, PositionId, Openings, EmploymentType, Location,
                    SalaryMin, SalaryMax, Description, Requirements, Status, OpenDate, CloseDate, CreatedBy)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                f.jobCode(), f.title(), f.departmentId(), f.positionId(), f.openings(), f.employmentType(), f.location(),
                f.salaryMin(), f.salaryMax(), f.description(), f.requirements(), f.status(), f.openDate(), f.closeDate(), createdBy);
    }

    @Override
    public void updateJob(long id, JobPostingForm f) {
        jdbc.update("""
                UPDATE dbo.JobPostings SET JobCode = ?, Title = ?, DepartmentId = ?, PositionId = ?, Openings = ?, EmploymentType = ?,
                    Location = ?, SalaryMin = ?, SalaryMax = ?, Description = ?, Requirements = ?, Status = ?, OpenDate = ?,
                    CloseDate = ?, UpdatedAt = SYSUTCDATETIME()
                WHERE JobPostingId = ?""",
                f.jobCode(), f.title(), f.departmentId(), f.positionId(), f.openings(), f.employmentType(), f.location(),
                f.salaryMin(), f.salaryMax(), f.description(), f.requirements(), f.status(), f.openDate(), f.closeDate(), id);
    }

    @Override
    public void updateJobStatus(long id, JobStatus status, LocalDate openDateIfMissing, LocalDate closeDate) {
        jdbc.update("""
                UPDATE dbo.JobPostings SET Status = ?, OpenDate = COALESCE(OpenDate, ?), CloseDate = COALESCE(?, CloseDate),
                    UpdatedAt = SYSUTCDATETIME()
                WHERE JobPostingId = ?""", status, openDateIfMissing, closeDate, id);
    }

    @Override
    public void deleteJob(long id) {
        jdbc.update("DELETE FROM dbo.JobPostings WHERE JobPostingId = ?", id);
    }

    @Override
    public boolean isJobInManagerScope(long jobPostingId, long managerEmployeeId) {
        return jdbc.count("SELECT COUNT(*) FROM dbo.JobPostings AS jp WHERE jp.JobPostingId = ? AND "
                        + String.format(MANAGER_SCOPE, "jp.DepartmentId", "jp.DepartmentId"),
                jobPostingId, managerEmployeeId, managerEmployeeId) > 0;
    }

    @Override
    public List<Candidate> candidatesOfJob(long jobPostingId) {
        return jdbc.query(CANDIDATE_SELECT + " WHERE ca.JobPostingId = ? ORDER BY ca.AppliedDate, ca.CandidateId",
                JdbcRecruitmentDao::mapCandidate, jobPostingId);
    }

    @Override
    public Page<Candidate> searchCandidates(Long jobPostingId, CandidateStage stage, String keyword, Long scopeManagerId,
                                            PageRequest page) {
        SqlWhere where = new SqlWhere()
                .andIfPresent("ca.JobPostingId = ?", jobPostingId)
                .andIfPresent("ca.Stage = ?", stage == null ? null : stage.name())
                .andContains(keyword, "ca.FullName", "ca.Email", "ca.Phone");
        if (scopeManagerId != null) {
            where.and(String.format(MANAGER_SCOPE, "jp.DepartmentId", "jp.DepartmentId"), scopeManagerId, scopeManagerId);
        }
        List<Candidate> items = jdbc.query(CANDIDATE_SELECT + where.toSql()
                        + SqlOrder.of(CANDIDATE_SORT, page, "ca.AppliedDate", "ca.CandidateId") + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                JdbcRecruitmentDao::mapCandidate, where.paramsWith(page.offset(), page.size()));
        long total = jdbc.count("SELECT COUNT(*) FROM dbo.Candidates AS ca JOIN dbo.JobPostings AS jp ON jp.JobPostingId = ca.JobPostingId"
                + where.toSql(), where.params().toArray());
        return Page.of(items, page, total);
    }

    @Override
    public Optional<Candidate> findCandidate(long candidateId) {
        return jdbc.queryOne(CANDIDATE_SELECT + " WHERE ca.CandidateId = ?", JdbcRecruitmentDao::mapCandidate, candidateId);
    }

    @Override
    public long insertCandidate(CandidateForm f) {
        return jdbc.insert("""
                INSERT INTO dbo.Candidates (JobPostingId, FullName, Email, Phone, Source, ResumeUrl, ExpectedSalary, Rating,
                    InterviewAt, Notes, AppliedDate)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", f.jobPostingId(), f.fullName(), f.email(), f.phone(), f.source(),
                f.resumeUrl(), f.expectedSalary(), f.rating(), f.interviewAt(), f.notes(), f.appliedDate());
    }

    @Override
    public void updateCandidate(long id, CandidateForm f) {
        jdbc.update("""
                UPDATE dbo.Candidates SET FullName = ?, Email = ?, Phone = ?, Source = ?, ResumeUrl = ?, ExpectedSalary = ?,
                    Rating = ?, InterviewAt = ?, Notes = ?, UpdatedAt = SYSUTCDATETIME()
                WHERE CandidateId = ?""", f.fullName(), f.email(), f.phone(), f.source(), f.resumeUrl(), f.expectedSalary(),
                f.rating(), f.interviewAt(), f.notes(), id);
    }

    @Override
    public void updateCandidateReview(long id, Integer rating, LocalDateTime interviewAt, String notes) {
        jdbc.update("UPDATE dbo.Candidates SET Rating = ?, InterviewAt = ?, Notes = ?, UpdatedAt = SYSUTCDATETIME() "
                + "WHERE CandidateId = ?", rating, interviewAt, notes, id);
    }

    @Override
    public void deleteCandidate(long id) {
        jdbc.update("DELETE FROM dbo.Candidates WHERE CandidateId = ?", id);
    }

    @Override
    public int changeStage(long id, CandidateStage expected, CandidateStage target, Long hiredEmployeeId) {
        return jdbc.update("UPDATE dbo.Candidates SET Stage = ?, HiredEmployeeId = COALESCE(?, HiredEmployeeId), "
                + "UpdatedAt = SYSUTCDATETIME() WHERE CandidateId = ? AND Stage = ?", target, hiredEmployeeId, id, expected);
    }

    @Override
    public List<StageChange> history(long candidateId) {
        return jdbc.query("SELECT FromStage, ToStage, ChangedBy, ChangedAt FROM dbo.CandidateStageHistory "
                        + "WHERE CandidateId = ? ORDER BY ChangedAt, HistoryId",
                rs -> new StageChange(rs.getString(1) == null ? null : CandidateStage.valueOf(rs.getString(1)),
                        CandidateStage.valueOf(rs.getString(2)), rs.getString(3), Rows.instant(rs, "ChangedAt")), candidateId);
    }

    @Override
    public Map<String, Long> recruitmentStats() {
        return jdbc.queryOne("""
                SELECT (SELECT COUNT(*) FROM dbo.JobPostings WHERE Status = 'OPEN') AS OpenJobs,
                       (SELECT ISNULL(SUM(Openings), 0) FROM dbo.JobPostings WHERE Status = 'OPEN') AS OpenPositions,
                       (SELECT COUNT(*) FROM dbo.Candidates WHERE Stage NOT IN ('HIRED', 'REJECTED', 'WITHDRAWN')) AS ActiveCandidates,
                       (SELECT COUNT(*) FROM dbo.Candidates WHERE Stage = 'INTERVIEW') AS Interviewing""", rs -> {
            Map<String, Long> m = new LinkedHashMap<>();
            m.put("openJobs", rs.getLong(1));
            m.put("openPositions", rs.getLong(2));
            m.put("activeCandidates", rs.getLong(3));
            m.put("interviewing", rs.getLong(4));
            return m;
        }).orElse(Map.of());
    }
}
