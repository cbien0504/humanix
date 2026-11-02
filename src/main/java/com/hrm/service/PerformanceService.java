package com.hrm.service;

import com.hrm.dao.PerformanceDao;
import com.hrm.dao.TransactionManager;
import com.hrm.exception.BusinessException;
import com.hrm.exception.ForbiddenException;
import com.hrm.exception.NotFoundException;
import com.hrm.exception.ValidationException;
import com.hrm.model.CycleStatus;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.model.PerformanceReview;
import com.hrm.model.ReviewCriterion;
import com.hrm.model.ReviewCycle;
import com.hrm.model.ReviewCycleForm;
import com.hrm.model.ReviewStatus;
import com.hrm.model.ScoreSubmission;
import com.hrm.security.AuthUser;
import com.hrm.security.Role;
import com.hrm.util.Validator;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performance management (UC-50 … UC-53, BR-50 … BR-53).
 * Cycle: DRAFT → OPEN (reviews generated) → CLOSED. Review: PENDING_SELF → PENDING_MANAGER → COMPLETED.
 */
public class PerformanceService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PerformanceDao dao;
    private final AuditService audit;
    private final TransactionManager tx;

    public PerformanceService(PerformanceDao dao, AuditService audit, TransactionManager tx) {
        this.dao = dao;
        this.audit = audit;
        this.tx = tx;
    }

    /** What the current user may do with a review (drives the form mode). */
    public record ReviewView(PerformanceReview review, boolean canSelfAssess, boolean canManagerAssess) {
    }

    // ------------------------------------------------------------------ cycles (HR)

    public List<ReviewCycle> cycles() {
        return dao.cycles();
    }

    public ReviewCycle cycle(long id) {
        return dao.cycle(id).orElseThrow(() -> NotFoundException.of("kỳ đánh giá", id));
    }

    public List<ReviewCriterion> criteria() {
        return dao.activeCriteria();
    }

    public ReviewCycle createCycle(AuthUser user, ReviewCycleForm form) {
        ReviewCycleForm f = normalize(form);
        validate(f);
        long id = dao.insertCycle(f, user.getUsername());
        audit.record("REVIEW_CYCLE_CREATED", "ReviewCycle", id, f.cycleName());
        return cycle(id);
    }

    public ReviewCycle updateCycle(long id, ReviewCycleForm form) {
        ReviewCycle current = cycle(id);
        if (current.getStatus() == CycleStatus.CLOSED) {
            throw new BusinessException("Kỳ đánh giá đã đóng, không thể sửa.");
        }
        ReviewCycleForm f = normalize(form);
        validate(f);
        dao.updateCycle(id, f);
        return cycle(id);
    }

    public void deleteCycle(long id) {
        ReviewCycle c = cycle(id);
        if (c.getStatus() != CycleStatus.DRAFT) {
            throw new BusinessException("Chỉ xóa được kỳ đánh giá ở trạng thái Nháp.");
        }
        dao.deleteCycle(id);
        audit.record("REVIEW_CYCLE_DELETED", "ReviewCycle", id, c.getCycleName());
    }

    /** Opens a draft cycle and creates the reviews of every eligible employee. */
    public ReviewCycle openCycle(long id) {
        ReviewCycle c = cycle(id);
        if (c.getStatus() != CycleStatus.DRAFT) {
            throw new BusinessException("Chỉ mở được kỳ đánh giá ở trạng thái Nháp.");
        }
        BigDecimal totalWeight = dao.activeCriteria().stream().map(ReviewCriterion::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(HUNDRED) != 0) {
            throw new BusinessException("Tổng trọng số các tiêu chí đang là " + totalWeight.stripTrailingZeros().toPlainString()
                    + "%, phải bằng 100% trước khi mở kỳ.");
        }
        int created = tx.inTransaction(() -> {
            if (dao.changeCycleStatus(id, CycleStatus.DRAFT, CycleStatus.OPEN) == 0) {
                throw new BusinessException("Kỳ đánh giá vừa được người khác cập nhật.");
            }
            return dao.generateReviews(id);
        });
        audit.record("REVIEW_CYCLE_OPENED", "ReviewCycle", id, created + " phiếu đánh giá");
        return cycle(id);
    }

    public ReviewCycle closeCycle(long id) {
        ReviewCycle c = cycle(id);
        if (c.getStatus() != CycleStatus.OPEN || dao.changeCycleStatus(id, CycleStatus.OPEN, CycleStatus.CLOSED) == 0) {
            throw new BusinessException("Chỉ đóng được kỳ đánh giá đang mở.");
        }
        audit.record("REVIEW_CYCLE_CLOSED", "ReviewCycle", id, c.getCompleted() + "/" + c.getTotalReviews() + " hoàn thành");
        return cycle(id);
    }

    public Map<String, Object> cycleStats(long id) {
        ReviewCycle c = cycle(id);
        Map<String, Object> m = new HashMap<>();
        m.put("cycle", c);
        m.put("ratings", dao.ratingDistribution(id));
        return m;
    }

    // ------------------------------------------------------------------ reviews

    public Page<PerformanceReview> myReviews(AuthUser user, PageRequest page) {
        return dao.searchReviews(null, null, null, requireEmployee(user), null, page);
    }

    /** HR: all reviews; managers: reviews where they are the reviewer. */
    public Page<PerformanceReview> reviews(AuthUser user, Long cycleId, ReviewStatus status, String keyword, PageRequest page) {
        if (user.isHrOrAdmin()) {
            return dao.searchReviews(cycleId, status, keyword, null, null, page);
        }
        if (user.hasRole(Role.MANAGER)) {
            return dao.searchReviews(cycleId, status, keyword, null, requireEmployee(user), page);
        }
        throw new ForbiddenException();
    }

    public ReviewView view(AuthUser user, long reviewId) {
        PerformanceReview r = load(reviewId);
        boolean owner = isSelf(user, r.getEmployeeId());
        boolean reviewer = r.getReviewerId() != null && isSelf(user, r.getReviewerId());
        if (!owner && !reviewer && !user.isHrOrAdmin()) {
            throw new ForbiddenException("Bạn không có quyền xem phiếu đánh giá này.");
        }
        boolean open = r.getCycleStatus() == CycleStatus.OPEN;
        return new ReviewView(r, owner && open && r.getStatus() == ReviewStatus.PENDING_SELF,
                reviewer && open && r.getStatus() == ReviewStatus.PENDING_MANAGER);
    }

    public PerformanceReview submitSelf(AuthUser user, long reviewId, ScoreSubmission submission) {
        PerformanceReview r = load(reviewId);
        if (!isSelf(user, r.getEmployeeId())) {
            throw new ForbiddenException("Chỉ nhân viên được đánh giá mới tự đánh giá được.");
        }
        requireOpen(r, ReviewStatus.PENDING_SELF, "Phiếu không ở bước tự đánh giá.");
        List<ReviewCriterion> criteria = dao.activeCriteria();
        Map<Long, Integer> scores = validateScores(submission, criteria);
        BigDecimal selfScore = PerformanceScoring.weightedAverage(scores, criteria);
        tx.inTransaction(() -> {
            if (dao.saveSelfAssessment(reviewId, scores, Validator.clean(submission.comment()), selfScore) == 0) {
                throw new BusinessException("Phiếu vừa được cập nhật ở nơi khác. Vui lòng tải lại.");
            }
        });
        audit.record("REVIEW_SELF_SUBMITTED", "PerformanceReview", reviewId, "điểm " + selfScore);
        return load(reviewId);
    }

    public PerformanceReview submitManager(AuthUser user, long reviewId, ScoreSubmission submission) {
        PerformanceReview r = load(reviewId);
        if (r.getReviewerId() == null || !isSelf(user, r.getReviewerId())) {
            throw new ForbiddenException("Chỉ người quản lý được phân công mới chấm điểm phiếu này.");
        }
        requireOpen(r, ReviewStatus.PENDING_MANAGER, "Phiếu chưa được nhân viên tự đánh giá hoặc đã hoàn thành.");
        List<ReviewCriterion> criteria = dao.activeCriteria();
        Map<Long, Integer> scores = validateScores(submission, criteria);
        BigDecimal managerScore = PerformanceScoring.weightedAverage(scores, criteria);
        BigDecimal finalScore = PerformanceScoring.finalScore(r.getSelfScore(), managerScore, r.getSelfWeight());
        String rating = PerformanceScoring.rating(finalScore);
        tx.inTransaction(() -> {
            if (dao.saveManagerAssessment(reviewId, scores, Validator.clean(submission.comment()), managerScore, finalScore, rating) == 0) {
                throw new BusinessException("Phiếu vừa được cập nhật ở nơi khác. Vui lòng tải lại.");
            }
        });
        audit.record("REVIEW_COMPLETED", "PerformanceReview", reviewId, r.getEmployeeCode() + " " + finalScore + " (" + rating + ")");
        return load(reviewId);
    }

    // ------------------------------------------------------------------ helpers

    private static Map<Long, Integer> validateScores(ScoreSubmission s, List<ReviewCriterion> criteria) {
        Map<String, String> errors = new HashMap<>();
        Map<Long, Integer> scores = s.scores() == null ? Map.of() : s.scores();
        for (ReviewCriterion c : criteria) {
            Integer score = scores.get(c.getCriteriaId());
            if (score == null || score < 1 || score > 5) {
                errors.put("score_" + c.getCriteriaId(), "Chấm điểm 1–5 cho \"" + c.getName() + "\".");
            }
        }
        if (s.comment() != null && s.comment().length() > 2000) {
            errors.put("comment", "Nhận xét tối đa 2000 ký tự.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return scores;
    }

    private static void requireOpen(PerformanceReview r, ReviewStatus expected, String message) {
        if (r.getCycleStatus() != CycleStatus.OPEN) {
            throw new BusinessException("Kỳ đánh giá không mở – không thể chỉnh sửa.");
        }
        if (r.getStatus() != expected) {
            throw new BusinessException(message);
        }
    }

    private PerformanceReview load(long id) {
        return dao.review(id).orElseThrow(() -> NotFoundException.of("phiếu đánh giá", id));
    }

    private static ReviewCycleForm normalize(ReviewCycleForm f) {
        return new ReviewCycleForm(Validator.clean(f.cycleName()), f.periodStart(), f.periodEnd(), f.dueDate(),
                f.selfWeight() == null ? new BigDecimal("0.30") : f.selfWeight());
    }

    private static void validate(ReviewCycleForm f) {
        new Validator()
                .required("cycleName", f.cycleName(), "Vui lòng nhập tên kỳ đánh giá.")
                .maxLength("cycleName", f.cycleName(), 150)
                .required("periodStart", f.periodStart(), "Vui lòng nhập ngày bắt đầu.")
                .required("periodEnd", f.periodEnd(), "Vui lòng nhập ngày kết thúc.")
                .check(f.periodStart() == null || f.periodEnd() == null || f.periodEnd().isAfter(f.periodStart()),
                        "periodEnd", "Ngày kết thúc phải sau ngày bắt đầu.")
                .notBefore("dueDate", f.dueDate(), f.periodStart(), "Hạn hoàn thành không hợp lệ.")
                .check(f.selfWeight().signum() >= 0 && f.selfWeight().compareTo(BigDecimal.ONE) <= 0, "selfWeight",
                        "Trọng số tự đánh giá từ 0 đến 1.")
                .throwIfInvalid();
    }

    private static boolean isSelf(AuthUser user, long employeeId) {
        return user.getEmployeeId() != null && user.getEmployeeId() == employeeId;
    }

    private static long requireEmployee(AuthUser user) {
        if (user.getEmployeeId() == null) {
            throw new ForbiddenException("Tài khoản của bạn chưa liên kết hồ sơ nhân viên.");
        }
        return user.getEmployeeId();
    }
}
