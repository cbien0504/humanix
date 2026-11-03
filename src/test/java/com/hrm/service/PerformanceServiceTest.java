package com.hrm.service;

import com.hrm.dao.PerformanceDao;
import com.hrm.exception.BusinessException;
import com.hrm.exception.ForbiddenException;
import com.hrm.exception.ValidationException;
import com.hrm.model.CycleStatus;
import com.hrm.model.PerformanceReview;
import com.hrm.model.ReviewCriterion;
import com.hrm.model.ReviewCycle;
import com.hrm.model.ReviewCycleForm;
import com.hrm.model.ReviewStatus;
import com.hrm.model.ScoreSubmission;
import com.hrm.testsupport.DirectTransactions;
import com.hrm.testsupport.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Scoring BR-51/BR-52 and the review workflow (UC-50 … UC-53). */
@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {

    /** Four criteria weighted 30 / 30 / 20 / 20 (the seed has five, 30/25/20/15/10 – the rule is the same). */
    private static final List<ReviewCriterion> CRITERIA = List.of(
            new ReviewCriterion(1, "Kết quả công việc", null, BigDecimal.valueOf(30)),
            new ReviewCriterion(2, "Chuyên môn", null, BigDecimal.valueOf(30)),
            new ReviewCriterion(3, "Tinh thần hợp tác", null, BigDecimal.valueOf(20)),
            new ReviewCriterion(4, "Kỷ luật", null, BigDecimal.valueOf(20)));
    private static final Map<Long, Integer> SCORES = Map.of(1L, 5, 2L, 4, 3L, 4, 4L, 3);   // → 4.10

    @Mock
    private PerformanceDao dao;
    @Mock
    private AuditService audit;

    private final DirectTransactions tx = new DirectTransactions();
    private PerformanceService service;

    @BeforeEach
    void setUp() {
        service = new PerformanceService(dao, audit, tx);
    }

    private static ReviewCycle cycle(long id, CycleStatus status) {
        ReviewCycle c = new ReviewCycle();
        c.setCycleId(id);
        c.setCycleName("Đánh giá 2026");
        c.setStatus(status);
        return c;
    }

    private static PerformanceReview review(long id, long employeeId, Long reviewerId, ReviewStatus status, CycleStatus cycle) {
        PerformanceReview r = new PerformanceReview();
        r.setReviewId(id);
        r.setEmployeeId(employeeId);
        r.setEmployeeCode("EMP00" + employeeId);
        r.setReviewerId(reviewerId);
        r.setStatus(status);
        r.setCycleStatus(cycle);
        r.setSelfWeight(new BigDecimal("0.30"));
        r.setSelfScore(new BigDecimal("4.35"));
        return r;
    }

    @Nested
    class Scoring {

        @Test
        void weightedAverageOfTheCriteria() {
            assertThat(PerformanceScoring.weightedAverage(SCORES, CRITERIA)).isEqualByComparingTo("4.10");
        }

        @Test
        void finalScoreBlendsSelfAndManagerScores() {
            // 0.3 × 4.35 + 0.7 × 4.10 = 4.175 → 4.18
            assertThat(PerformanceScoring.finalScore(new BigDecimal("4.35"), new BigDecimal("4.10"), new BigDecimal("0.30")))
                    .isEqualByComparingTo("4.18");
        }

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({"5.00, A", "4.50, A", "4.49, B", "3.50, B", "3.49, C", "2.50, C", "1.50, D", "1.49, E", "1.00, E"})
        void ratingBands(BigDecimal score, String rating) {
            assertThat(PerformanceScoring.rating(score)).isEqualTo(rating);
        }

        @Test
        void everyCriterionNeedsAScore() {
            assertThatThrownBy(() -> PerformanceScoring.weightedAverage(Map.of(1L, 5), CRITERIA))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Cycles {

        @Test
        void openingGeneratesTheReviewsAtomically() {
            when(dao.cycle(2)).thenReturn(Optional.of(cycle(2, CycleStatus.DRAFT)));
            when(dao.activeCriteria()).thenReturn(CRITERIA);
            when(dao.changeCycleStatus(2, CycleStatus.DRAFT, CycleStatus.OPEN)).thenReturn(1);
            when(dao.generateReviews(2)).thenReturn(34);

            service.openCycle(2);

            assertThat(tx.committed()).isEqualTo(1);
            verify(audit).record("REVIEW_CYCLE_OPENED", "ReviewCycle", 2L, "34 phiếu đánh giá");
        }

        @Test
        void criteriaWeightsMustTotalOneHundredBeforeOpening() {
            when(dao.cycle(2)).thenReturn(Optional.of(cycle(2, CycleStatus.DRAFT)));
            when(dao.activeCriteria()).thenReturn(CRITERIA.subList(0, 3));          // 80 %

            assertThatThrownBy(() -> service.openCycle(2))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("80%");
            verify(dao, never()).generateReviews(anyLong());
        }

        @Test
        void aCycleOpenedConcurrentlyIsNotOpenedTwice() {
            when(dao.cycle(2)).thenReturn(Optional.of(cycle(2, CycleStatus.DRAFT)));
            when(dao.activeCriteria()).thenReturn(CRITERIA);
            when(dao.changeCycleStatus(2, CycleStatus.DRAFT, CycleStatus.OPEN)).thenReturn(0);

            assertThatThrownBy(() -> service.openCycle(2)).isInstanceOf(BusinessException.class);
            assertThat(tx.rolledBack()).isEqualTo(1);
            verify(dao, never()).generateReviews(anyLong());
        }

        @Test
        void lifecycleGuards() {
            when(dao.cycle(1)).thenReturn(Optional.of(cycle(1, CycleStatus.CLOSED)));
            when(dao.cycle(2)).thenReturn(Optional.of(cycle(2, CycleStatus.OPEN)));
            ReviewCycleForm form = new ReviewCycleForm("Đánh giá 2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                    LocalDate.of(2027, 1, 15), null);

            assertThatThrownBy(() -> service.updateCycle(1, form)).hasMessageContaining("đã đóng");
            assertThatThrownBy(() -> service.closeCycle(1)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> service.deleteCycle(2)).hasMessageContaining("Nháp");
            assertThatThrownBy(() -> service.openCycle(2)).hasMessageContaining("Nháp");
        }

        @Test
        void newCycleDefaultsSelfWeightAndValidatesDates() {
            when(dao.insertCycle(any(), eq("hr"))).thenReturn(5L);
            when(dao.cycle(5)).thenReturn(Optional.of(cycle(5, CycleStatus.DRAFT)));

            service.createCycle(Fixtures.hr(5), new ReviewCycleForm(" Đánh giá 2026 ", LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31), null, null));

            ArgumentCaptor<ReviewCycleForm> stored = ArgumentCaptor.forClass(ReviewCycleForm.class);
            verify(dao).insertCycle(stored.capture(), eq("hr"));
            assertThat(stored.getValue().cycleName()).isEqualTo("Đánh giá 2026");
            assertThat(stored.getValue().selfWeight()).isEqualByComparingTo("0.30");

            assertThatThrownBy(() -> service.createCycle(Fixtures.hr(5), new ReviewCycleForm("X", LocalDate.of(2026, 12, 31),
                    LocalDate.of(2026, 1, 1), null, new BigDecimal("1.5"))))
                    .isInstanceOfSatisfying(ValidationException.class,
                            e -> assertThat(e.getFieldErrors()).containsKeys("periodEnd", "selfWeight"));
        }
    }

    @Nested
    class Reviews {

        @Test
        void employeeSubmitsHisSelfAssessment() {
            when(dao.review(9)).thenReturn(Optional.of(review(9, 16, 13L, ReviewStatus.PENDING_SELF, CycleStatus.OPEN)));
            when(dao.activeCriteria()).thenReturn(CRITERIA);
            when(dao.saveSelfAssessment(eq(9L), eq(SCORES), eq("Hoàn thành tốt"), any())).thenReturn(1);

            service.submitSelf(Fixtures.employee(16), 9, new ScoreSubmission(SCORES, " Hoàn thành tốt "));

            ArgumentCaptor<BigDecimal> score = ArgumentCaptor.forClass(BigDecimal.class);
            verify(dao).saveSelfAssessment(eq(9L), eq(SCORES), eq("Hoàn thành tốt"), score.capture());
            assertThat(score.getValue()).isEqualByComparingTo("4.10");
        }

        @Test
        void managerScoreCompletesTheReviewWithFinalScoreAndRating() {
            when(dao.review(9)).thenReturn(Optional.of(review(9, 16, 13L, ReviewStatus.PENDING_MANAGER, CycleStatus.OPEN)));
            when(dao.activeCriteria()).thenReturn(CRITERIA);
            when(dao.saveManagerAssessment(eq(9L), eq(SCORES), eq(null), any(), any(), eq("B"))).thenReturn(1);

            service.submitManager(Fixtures.manager(13), 9, new ScoreSubmission(SCORES, null));

            ArgumentCaptor<BigDecimal> finalScore = ArgumentCaptor.forClass(BigDecimal.class);
            verify(dao).saveManagerAssessment(eq(9L), eq(SCORES), eq(null), any(), finalScore.capture(), eq("B"));
            assertThat(finalScore.getValue()).isEqualByComparingTo("4.18");
            verify(audit).record("REVIEW_COMPLETED", "PerformanceReview", 9L, "EMP0016 4.18 (B)");
        }

        @Test
        void onlyTheAssignedReviewerScores() {
            when(dao.review(9)).thenReturn(Optional.of(review(9, 16, 13L, ReviewStatus.PENDING_MANAGER, CycleStatus.OPEN)));

            assertThatThrownBy(() -> service.submitManager(Fixtures.manager(11), 9, new ScoreSubmission(SCORES, null)))
                    .isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> service.submitSelf(Fixtures.employee(17), 9, new ScoreSubmission(SCORES, null)))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void stepsHappenInOrderAndOnlyWhileTheCycleIsOpen() {
            when(dao.review(9)).thenReturn(Optional.of(review(9, 16, 13L, ReviewStatus.PENDING_SELF, CycleStatus.OPEN)));
            when(dao.review(10)).thenReturn(Optional.of(review(10, 16, 13L, ReviewStatus.PENDING_SELF, CycleStatus.CLOSED)));

            assertThatThrownBy(() -> service.submitManager(Fixtures.manager(13), 9, new ScoreSubmission(SCORES, null)))
                    .hasMessageContaining("chưa được nhân viên tự đánh giá");
            assertThatThrownBy(() -> service.submitSelf(Fixtures.employee(16), 10, new ScoreSubmission(SCORES, null)))
                    .hasMessageContaining("không mở");
        }

        @Test
        void missingOrOutOfRangeScoresArePointedOutPerCriterion() {
            when(dao.review(9)).thenReturn(Optional.of(review(9, 16, 13L, ReviewStatus.PENDING_SELF, CycleStatus.OPEN)));
            when(dao.activeCriteria()).thenReturn(CRITERIA);

            assertThatThrownBy(() -> service.submitSelf(Fixtures.employee(16), 9,
                    new ScoreSubmission(Map.of(1L, 6, 2L, 4, 3L, 0), null)))
                    .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getFieldErrors())
                            .containsOnlyKeys("score_1", "score_3", "score_4"));
            verify(dao, never()).saveSelfAssessment(anyLong(), any(), anyString(), any());
        }

        @Test
        void aReviewEditedElsewhereIsNotOverwritten() {
            when(dao.review(9)).thenReturn(Optional.of(review(9, 16, 13L, ReviewStatus.PENDING_SELF, CycleStatus.OPEN)));
            when(dao.activeCriteria()).thenReturn(CRITERIA);
            when(dao.saveSelfAssessment(anyLong(), any(), any(), any())).thenReturn(0);

            assertThatThrownBy(() -> service.submitSelf(Fixtures.employee(16), 9, new ScoreSubmission(SCORES, null)))
                    .hasMessageContaining("cập nhật ở nơi khác");
        }

        @Test
        void viewPermissionsFollowOwnerReviewerAndHr() {
            when(dao.review(9)).thenReturn(Optional.of(review(9, 16, 13L, ReviewStatus.PENDING_SELF, CycleStatus.OPEN)));

            assertThat(service.view(Fixtures.employee(16), 9).canSelfAssess()).isTrue();
            assertThat(service.view(Fixtures.manager(13), 9).canManagerAssess()).isFalse();   // not his step yet
            assertThat(service.view(Fixtures.hr(5), 9).canSelfAssess()).isFalse();
            assertThatThrownBy(() -> service.view(Fixtures.employee(17), 9)).isInstanceOf(ForbiddenException.class);
        }

        @Test
        void reviewListsAreScopedByRole() {
            service.reviews(Fixtures.hr(5), 2L, null, null, null);
            service.reviews(Fixtures.manager(13), 2L, ReviewStatus.PENDING_MANAGER, null, null);

            verify(dao).searchReviews(2L, null, null, null, null, null);
            verify(dao).searchReviews(2L, ReviewStatus.PENDING_MANAGER, null, null, 13L, null);
            assertThatThrownBy(() -> service.reviews(Fixtures.employee(16), null, null, null, null))
                    .isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> service.myReviews(Fixtures.admin(), null)).isInstanceOf(ForbiddenException.class);
        }
    }
}
