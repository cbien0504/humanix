package com.hrm.dao;

import com.hrm.model.CycleStatus;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.model.PerformanceReview;
import com.hrm.model.ReviewCriterion;
import com.hrm.model.ReviewCycle;
import com.hrm.model.ReviewCycleForm;
import com.hrm.model.ReviewStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PerformanceDao {

    List<ReviewCycle> cycles();

    Optional<ReviewCycle> cycle(long cycleId);

    long insertCycle(ReviewCycleForm form, String createdBy);

    void updateCycle(long cycleId, ReviewCycleForm form);

    void deleteCycle(long cycleId);

    int changeCycleStatus(long cycleId, CycleStatus expected, CycleStatus target);

    int generateReviews(long cycleId);

    List<ReviewCriterion> activeCriteria();

    Page<PerformanceReview> searchReviews(Long cycleId, ReviewStatus status, String keyword, Long employeeId,
                                          Long reviewerId, PageRequest page);

    Optional<PerformanceReview> review(long reviewId);

    int saveSelfAssessment(long reviewId, Map<Long, Integer> scores, String comment, BigDecimal selfScore);

    int saveManagerAssessment(long reviewId, Map<Long, Integer> scores, String comment, BigDecimal managerScore,
                              BigDecimal finalScore, String rating);

    Map<String, Integer> ratingDistribution(long cycleId);
}
