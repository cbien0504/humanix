package com.hrm.dao.jdbc;

import com.hrm.dao.PerformanceDao;
import com.hrm.model.CycleStatus;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.model.PerformanceReview;
import com.hrm.model.ReviewCriterion;
import com.hrm.model.ReviewCycle;
import com.hrm.model.ReviewCycleForm;
import com.hrm.model.ReviewStatus;
import com.hrm.model.ScoreLine;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcPerformanceDao implements PerformanceDao {

    private static final String CYCLE_SELECT = """
            SELECT c.CycleId, c.CycleName, c.PeriodStart, c.PeriodEnd, c.DueDate, c.SelfWeight, c.Status, c.CreatedBy,
                   ISNULL(s.Total, 0) AS Total, ISNULL(s.PendingSelf, 0) AS PendingSelf,
                   ISNULL(s.PendingManager, 0) AS PendingManager, ISNULL(s.Completed, 0) AS Completed, s.AvgScore
            FROM dbo.ReviewCycles AS c
            OUTER APPLY (SELECT COUNT(*) AS Total,
                                SUM(CASE WHEN r.Status = 'PENDING_SELF' THEN 1 ELSE 0 END) AS PendingSelf,
                                SUM(CASE WHEN r.Status = 'PENDING_MANAGER' THEN 1 ELSE 0 END) AS PendingManager,
                                SUM(CASE WHEN r.Status = 'COMPLETED' THEN 1 ELSE 0 END) AS Completed,
                                CAST(AVG(r.FinalScore) AS DECIMAL(3,2)) AS AvgScore
                         FROM dbo.PerformanceReviews AS r WHERE r.CycleId = c.CycleId) AS s
            """;

    private static final String REVIEW_SELECT = """
            SELECT r.ReviewId, r.CycleId, c.CycleName, c.Status AS CycleStatus, c.PeriodStart, c.PeriodEnd, c.DueDate,
                   c.SelfWeight, r.EmployeeId, e.EmployeeCode, e.FullName, d.DepartmentName, p.PositionName,
                   r.ReviewerId, rv.FullName AS ReviewerName, r.Status, r.SelfScore, r.ManagerScore, r.FinalScore, r.Rating,
                   r.SelfComment, r.ManagerComment, r.SelfSubmittedAt, r.ManagerSubmittedAt
            FROM dbo.PerformanceReviews AS r
            JOIN dbo.ReviewCycles AS c ON c.CycleId = r.CycleId
            JOIN dbo.Employees AS e ON e.EmployeeId = r.EmployeeId
            JOIN dbo.Departments AS d ON d.DepartmentId = e.DepartmentId
            JOIN dbo.Positions AS p ON p.PositionId = e.PositionId
            LEFT JOIN dbo.Employees AS rv ON rv.EmployeeId = r.ReviewerId
            """;

    private static final Map<String, String> SORT = Map.of(
            "employee", "e.FullName", "code", "e.EmployeeCode", "department", "d.DepartmentName", "status", "r.Status",
            "final", "r.FinalScore", "cycle", "c.PeriodStart");

    private final JdbcExecutor jdbc;

    public JdbcPerformanceDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    static ReviewCycle mapCycle(ResultSet rs) throws SQLException {
        ReviewCycle c = new ReviewCycle();
        c.setCycleId(rs.getLong("CycleId"));
        c.setCycleName(rs.getString("CycleName"));
        c.setPeriodStart(Rows.date(rs, "PeriodStart"));
        c.setPeriodEnd(Rows.date(rs, "PeriodEnd"));
        c.setDueDate(Rows.date(rs, "DueDate"));
        c.setSelfWeight(rs.getBigDecimal("SelfWeight"));
        c.setStatus(CycleStatus.valueOf(rs.getString("Status")));
        c.setCreatedBy(rs.getString("CreatedBy"));
        c.setTotalReviews(rs.getInt("Total"));
        c.setPendingSelf(rs.getInt("PendingSelf"));
        c.setPendingManager(rs.getInt("PendingManager"));
        c.setCompleted(rs.getInt("Completed"));
        c.setAverageScore(rs.getBigDecimal("AvgScore"));
        return c;
    }

    static PerformanceReview mapReview(ResultSet rs) throws SQLException {
        PerformanceReview r = new PerformanceReview();
        r.setReviewId(rs.getLong("ReviewId"));
        r.setCycleId(rs.getLong("CycleId"));
        r.setCycleName(rs.getString("CycleName"));
        r.setCycleStatus(CycleStatus.valueOf(rs.getString("CycleStatus")));
        r.setPeriodStart(Rows.date(rs, "PeriodStart"));
        r.setPeriodEnd(Rows.date(rs, "PeriodEnd"));
        r.setDueDate(Rows.date(rs, "DueDate"));
        r.setSelfWeight(rs.getBigDecimal("SelfWeight"));
        r.setEmployeeId(rs.getLong("EmployeeId"));
        r.setEmployeeCode(rs.getString("EmployeeCode"));
        r.setFullName(rs.getString("FullName"));
        r.setDepartmentName(rs.getString("DepartmentName"));
        r.setPositionName(rs.getString("PositionName"));
        r.setReviewerId(Rows.longValue(rs, "ReviewerId"));
        r.setReviewerName(rs.getString("ReviewerName"));
        r.setStatus(ReviewStatus.valueOf(rs.getString("Status")));
        r.setSelfScore(rs.getBigDecimal("SelfScore"));
        r.setManagerScore(rs.getBigDecimal("ManagerScore"));
        r.setFinalScore(rs.getBigDecimal("FinalScore"));
        r.setRating(rs.getString("Rating"));
        r.setSelfComment(rs.getString("SelfComment"));
        r.setManagerComment(rs.getString("ManagerComment"));
        r.setSelfSubmittedAt(Rows.instant(rs, "SelfSubmittedAt"));
        r.setManagerSubmittedAt(Rows.instant(rs, "ManagerSubmittedAt"));
        return r;
    }

    @Override
    public List<ReviewCycle> cycles() {
        return jdbc.query(CYCLE_SELECT + " ORDER BY c.PeriodStart DESC", JdbcPerformanceDao::mapCycle);
    }

    @Override
    public Optional<ReviewCycle> cycle(long cycleId) {
        return jdbc.queryOne(CYCLE_SELECT + " WHERE c.CycleId = ?", JdbcPerformanceDao::mapCycle, cycleId);
    }

    @Override
    public long insertCycle(ReviewCycleForm f, String createdBy) {
        return jdbc.insert("INSERT INTO dbo.ReviewCycles (CycleName, PeriodStart, PeriodEnd, DueDate, SelfWeight, CreatedBy) "
                + "VALUES (?, ?, ?, ?, ?, ?)", f.cycleName(), f.periodStart(), f.periodEnd(), f.dueDate(), f.selfWeight(), createdBy);
    }

    @Override
    public void updateCycle(long cycleId, ReviewCycleForm f) {
        jdbc.update("UPDATE dbo.ReviewCycles SET CycleName = ?, PeriodStart = ?, PeriodEnd = ?, DueDate = ?, SelfWeight = ?, "
                + "UpdatedAt = SYSUTCDATETIME() WHERE CycleId = ?", f.cycleName(), f.periodStart(), f.periodEnd(), f.dueDate(),
                f.selfWeight(), cycleId);
    }

    @Override
    public void deleteCycle(long cycleId) {
        jdbc.update("DELETE FROM dbo.ReviewCycles WHERE CycleId = ?", cycleId);
    }

    @Override
    public int changeCycleStatus(long cycleId, CycleStatus expected, CycleStatus target) {
        return jdbc.update("UPDATE dbo.ReviewCycles SET Status = ?, UpdatedAt = SYSUTCDATETIME() WHERE CycleId = ? AND Status = ?",
                target, cycleId, expected);
    }

    @Override
    public int generateReviews(long cycleId) {
        return jdbc.update("""
                INSERT INTO dbo.PerformanceReviews (CycleId, EmployeeId, ReviewerId, Status)
                SELECT c.CycleId, e.EmployeeId, rv.ReviewerId, 'PENDING_SELF'
                FROM dbo.ReviewCycles AS c
                JOIN dbo.Employees AS e ON e.HireDate <= c.PeriodEnd AND e.Status IN ('ACTIVE', 'PROBATION')
                JOIN dbo.Departments AS d ON d.DepartmentId = e.DepartmentId
                CROSS APPLY (SELECT COALESCE(e.ManagerId, d.ManagerId) AS ReviewerId) AS rv
                WHERE c.CycleId = ? AND rv.ReviewerId IS NOT NULL AND rv.ReviewerId <> e.EmployeeId
                  AND NOT EXISTS (SELECT 1 FROM dbo.PerformanceReviews AS x WHERE x.CycleId = c.CycleId AND x.EmployeeId = e.EmployeeId)""",
                cycleId);
    }

    @Override
    public List<ReviewCriterion> activeCriteria() {
        return jdbc.query("SELECT CriteriaId, CriteriaName, Description, Weight FROM dbo.ReviewCriteria WHERE IsActive = 1 "
                + "ORDER BY DisplayOrder, CriteriaId", rs -> new ReviewCriterion(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getBigDecimal(4)));
    }

    @Override
    public Page<PerformanceReview> searchReviews(Long cycleId, ReviewStatus status, String keyword, Long employeeId,
                                                 Long reviewerId, PageRequest page) {
        SqlWhere where = new SqlWhere()
                .andIfPresent("r.CycleId = ?", cycleId)
                .andIfPresent("r.Status = ?", status == null ? null : status.name())
                .andContains(keyword, "e.EmployeeCode", "e.FullName")
                .andIfPresent("r.EmployeeId = ?", employeeId)
                .andIfPresent("r.ReviewerId = ?", reviewerId);
        List<PerformanceReview> items = jdbc.query(REVIEW_SELECT + where.toSql()
                        + SqlOrder.of(SORT, page, "c.PeriodStart DESC, e.EmployeeCode", "r.ReviewId") + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                JdbcPerformanceDao::mapReview, where.paramsWith(page.offset(), page.size()));
        long total = jdbc.count("SELECT COUNT(*) FROM dbo.PerformanceReviews AS r JOIN dbo.ReviewCycles AS c ON c.CycleId = r.CycleId "
                + "JOIN dbo.Employees AS e ON e.EmployeeId = r.EmployeeId JOIN dbo.Departments AS d ON d.DepartmentId = e.DepartmentId"
                + where.toSql(), where.params().toArray());
        return Page.of(items, page, total);
    }

    @Override
    public Optional<PerformanceReview> review(long reviewId) {
        Optional<PerformanceReview> review = jdbc.queryOne(REVIEW_SELECT + " WHERE r.ReviewId = ?", JdbcPerformanceDao::mapReview, reviewId);
        review.ifPresent(r -> r.setScores(jdbc.query("""
                SELECT rc.CriteriaId, rc.CriteriaName, rc.Description, rc.Weight, rs.SelfScore, rs.ManagerScore
                FROM dbo.ReviewCriteria AS rc
                LEFT JOIN dbo.ReviewScores AS rs ON rs.CriteriaId = rc.CriteriaId AND rs.ReviewId = ?
                WHERE rc.IsActive = 1 OR rs.ReviewId IS NOT NULL
                ORDER BY rc.DisplayOrder, rc.CriteriaId""",
                rs -> new ScoreLine(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBigDecimal(4),
                        Rows.integer(rs, "SelfScore"), Rows.integer(rs, "ManagerScore")), reviewId)));
        return review;
    }

    @Override
    public int saveSelfAssessment(long reviewId, Map<Long, Integer> scores, String comment, BigDecimal selfScore) {
        int updated = jdbc.update("UPDATE dbo.PerformanceReviews SET Status = 'PENDING_MANAGER', SelfScore = ?, SelfComment = ?, "
                + "SelfSubmittedAt = SYSUTCDATETIME(), UpdatedAt = SYSUTCDATETIME() WHERE ReviewId = ? AND Status = 'PENDING_SELF'",
                selfScore, comment, reviewId);
        if (updated == 1) {
            List<Object[]> rows = new ArrayList<>();
            scores.forEach((criteriaId, score) -> rows.add(new Object[]{reviewId, criteriaId, score}));
            jdbc.batch("""
                    MERGE dbo.ReviewScores WITH (HOLDLOCK) AS t
                    USING (SELECT ? AS ReviewId, ? AS CriteriaId, ? AS Score) AS s
                       ON t.ReviewId = s.ReviewId AND t.CriteriaId = s.CriteriaId
                    WHEN MATCHED THEN UPDATE SET SelfScore = s.Score
                    WHEN NOT MATCHED THEN INSERT (ReviewId, CriteriaId, SelfScore) VALUES (s.ReviewId, s.CriteriaId, s.Score);""", rows);
        }
        return updated;
    }

    @Override
    public int saveManagerAssessment(long reviewId, Map<Long, Integer> scores, String comment, BigDecimal managerScore,
                                     BigDecimal finalScore, String rating) {
        List<Object[]> rows = new ArrayList<>();
        scores.forEach((criteriaId, score) -> rows.add(new Object[]{reviewId, criteriaId, score}));
        jdbc.batch("""
                MERGE dbo.ReviewScores WITH (HOLDLOCK) AS t
                USING (SELECT ? AS ReviewId, ? AS CriteriaId, ? AS Score) AS s
                   ON t.ReviewId = s.ReviewId AND t.CriteriaId = s.CriteriaId
                WHEN MATCHED THEN UPDATE SET ManagerScore = s.Score
                WHEN NOT MATCHED THEN INSERT (ReviewId, CriteriaId, ManagerScore) VALUES (s.ReviewId, s.CriteriaId, s.Score);""", rows);
        return jdbc.update("UPDATE dbo.PerformanceReviews SET Status = 'COMPLETED', ManagerScore = ?, FinalScore = ?, Rating = ?, "
                + "ManagerComment = ?, ManagerSubmittedAt = SYSUTCDATETIME(), UpdatedAt = SYSUTCDATETIME() "
                + "WHERE ReviewId = ? AND Status = 'PENDING_MANAGER'", managerScore, finalScore, rating, comment, reviewId);
    }

    @Override
    public Map<String, Integer> ratingDistribution(long cycleId) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String r : List.of("A", "B", "C", "D", "E")) {
            m.put(r, 0);
        }
        jdbc.query("SELECT Rating, COUNT(*) FROM dbo.PerformanceReviews WHERE CycleId = ? AND Rating IS NOT NULL GROUP BY Rating",
                rs -> m.put(rs.getString(1), rs.getInt(2)), cycleId);
        return m;
    }
}
