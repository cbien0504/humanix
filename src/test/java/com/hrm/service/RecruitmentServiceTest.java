package com.hrm.service;

import com.hrm.dao.DepartmentDao;
import com.hrm.dao.EmployeeDao;
import com.hrm.dao.PositionDao;
import com.hrm.dao.RecruitmentDao;
import com.hrm.exception.BusinessException;
import com.hrm.exception.ForbiddenException;
import com.hrm.exception.ValidationException;
import com.hrm.model.Candidate;
import com.hrm.model.CandidateForm;
import com.hrm.model.CandidateStage;
import com.hrm.model.Department;
import com.hrm.model.Employee;
import com.hrm.model.EmployeeForm;
import com.hrm.model.EmployeeStatus;
import com.hrm.model.HireRequest;
import com.hrm.model.JobPosting;
import com.hrm.model.JobPostingForm;
import com.hrm.model.JobStatus;
import com.hrm.testsupport.DirectTransactions;
import com.hrm.testsupport.Fixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.hrm.testsupport.Fixtures.vnd;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Job postings, candidate pipeline BR-42 and hiring BR-43 (UC-40 … UC-43). */
@ExtendWith(MockitoExtension.class)
class RecruitmentServiceTest {

    @Mock
    private RecruitmentDao recruitmentDao;
    @Mock
    private DepartmentDao departmentDao;
    @Mock
    private PositionDao positionDao;
    @Mock
    private EmployeeDao employeeDao;
    @Mock
    private AuditService audit;

    private final DirectTransactions tx = new DirectTransactions();
    private final Locale originalLocale = Locale.getDefault();
    private RecruitmentService service;

    @BeforeEach
    void setUp() {
        EmployeeService employees = new EmployeeService(employeeDao, departmentDao, positionDao, audit, tx, Fixtures.clock());
        service = new RecruitmentService(recruitmentDao, departmentDao, positionDao, employees, audit, tx, Fixtures.clock());
        Department it = Fixtures.department(4, "IT", true);
        it.setManagerId(11L);
        lenient().when(departmentDao.findById(4)).thenReturn(Optional.of(it));
        lenient().when(positionDao.findById(6)).thenReturn(Optional.of(Fixtures.position(6, "DEV", 12_000_000, 28_000_000)));
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    private static JobPosting job(long id, JobStatus status, int openings, int hired) {
        JobPosting j = new JobPosting();
        j.setJobPostingId(id);
        j.setJobCode("JOB-DEV-01");
        j.setDepartmentId(4);
        j.setPositionId(6);
        j.setStatus(status);
        j.setOpenings(openings);
        j.setHiredCount(hired);
        return j;
    }

    private static Candidate candidate(long id, CandidateStage stage) {
        Candidate c = new Candidate();
        c.setCandidateId(id);
        c.setJobPostingId(3);
        c.setFullName("Trần Minh Khoa");
        c.setEmail("khoa.tm@example.com");
        c.setPhone("0987000111");
        c.setExpectedSalary(vnd(20_000_000));
        c.setStage(stage);
        return c;
    }

    private static CandidateForm candidateForm(String source, String resumeUrl) {
        return new CandidateForm(3L, "Trần Minh Khoa", "Khoa.TM@Example.com", "0987000111", source, resumeUrl,
                vnd(20_000_000), null, null, null, null);
    }

    @Nested
    class Pipeline {

        @Test
        void candidatesMoveOneStepForwardOrLeave() {
            when(recruitmentDao.findCandidate(8)).thenReturn(Optional.of(candidate(8, CandidateStage.SCREENING)));
            when(recruitmentDao.changeStage(8, CandidateStage.SCREENING, CandidateStage.INTERVIEW, null)).thenReturn(1);

            service.moveStage(Fixtures.hr(5), 8, CandidateStage.INTERVIEW);

            verify(audit).record("CANDIDATE_STAGE_CHANGED", "Candidate", 8L, "SCREENING → INTERVIEW");
        }

        @Test
        void stagesCannotBeSkippedOrReopened() {
            when(recruitmentDao.findCandidate(8)).thenReturn(Optional.of(candidate(8, CandidateStage.APPLIED)));
            when(recruitmentDao.findCandidate(9)).thenReturn(Optional.of(candidate(9, CandidateStage.REJECTED)));

            assertThatThrownBy(() -> service.moveStage(Fixtures.hr(5), 8, CandidateStage.OFFER))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Mới nộp");
            assertThatThrownBy(() -> service.moveStage(Fixtures.hr(5), 9, CandidateStage.SCREENING))
                    .isInstanceOf(BusinessException.class);
            verify(recruitmentDao, never()).changeStage(anyLong(), any(), any(), any());
        }

        @Test
        void hiringHasItsOwnAction() {
            when(recruitmentDao.findCandidate(8)).thenReturn(Optional.of(candidate(8, CandidateStage.OFFER)));

            assertThatThrownBy(() -> service.moveStage(Fixtures.hr(5), 8, CandidateStage.HIRED))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Tiếp nhận");
        }

        @Test
        void aConcurrentMoveIsDetected() {
            when(recruitmentDao.findCandidate(8)).thenReturn(Optional.of(candidate(8, CandidateStage.SCREENING)));
            when(recruitmentDao.changeStage(8, CandidateStage.SCREENING, CandidateStage.INTERVIEW, null)).thenReturn(0);

            assertThatThrownBy(() -> service.moveStage(Fixtures.hr(5), 8, CandidateStage.INTERVIEW))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("người khác cập nhật");
        }

        @Test
        void detailOffersOnlyTheReachableStagesExceptHired() {
            when(recruitmentDao.findCandidate(8)).thenReturn(Optional.of(candidate(8, CandidateStage.OFFER)));

            assertThat(service.candidate(Fixtures.hr(5), 8).nextStages())
                    .containsExactlyInAnyOrder(CandidateStage.REJECTED, CandidateStage.WITHDRAWN);
        }

        @Test
        void boardHasOneColumnPerStage() {
            when(recruitmentDao.findJob(3)).thenReturn(Optional.of(job(3, JobStatus.OPEN, 2, 0)));
            when(recruitmentDao.candidatesOfJob(3)).thenReturn(List.of(candidate(8, CandidateStage.INTERVIEW),
                    candidate(9, CandidateStage.INTERVIEW), candidate(10, CandidateStage.APPLIED)));

            var board = service.pipeline(Fixtures.hr(5), 3);

            assertThat(board.columns()).hasSize(CandidateStage.values().length);
            assertThat(board.columns().get(CandidateStage.INTERVIEW)).hasSize(2);
            assertThat(board.columns().get(CandidateStage.OFFER)).isEmpty();
        }
    }

    @Nested
    class Hire {

        @Test
        void createsAProbationaryEmployeeAndClosesTheFilledPosting() {
            when(recruitmentDao.findCandidate(8)).thenReturn(Optional.of(candidate(8, CandidateStage.OFFER)));
            when(recruitmentDao.findJob(3)).thenReturn(Optional.of(job(3, JobStatus.OPEN, 1, 0)));
            when(employeeDao.nextCode("EMP")).thenReturn("EMP0037");
            when(employeeDao.findById(11)).thenReturn(Optional.of(Fixtures.employeeRecord(11, "EMP0011")));
            when(employeeDao.insert(any())).thenReturn(37L);
            when(employeeDao.findById(37)).thenReturn(Optional.of(Fixtures.employeeRecord(37, "EMP0037")));
            when(recruitmentDao.changeStage(8, CandidateStage.OFFER, CandidateStage.HIRED, 37L)).thenReturn(1);

            Employee hired = service.hire(8, new HireRequest(null, "M", LocalDate.of(1996, 4, 2), null, null, null, null));

            assertThat(hired.getEmployeeCode()).isEqualTo("EMP0037");
            ArgumentCaptor<EmployeeForm> form = ArgumentCaptor.forClass(EmployeeForm.class);
            verify(employeeDao).insert(form.capture());
            assertThat(form.getValue().status()).isEqualTo(EmployeeStatus.PROBATION);
            assertThat(form.getValue().managerId()).as("department manager by default").isEqualTo(11L);
            assertThat(form.getValue().baseSalary()).as("expected salary by default").isEqualByComparingTo("20000000");
            assertThat(form.getValue().hireDate()).isEqualTo(Fixtures.TODAY);
            verify(recruitmentDao).updateJobStatus(3, JobStatus.CLOSED, null, Fixtures.TODAY);
            assertThat(tx.committed()).isEqualTo(1);
        }

        @Test
        void onlyCandidatesWithAnOfferCanBeHired() {
            when(recruitmentDao.findCandidate(8)).thenReturn(Optional.of(candidate(8, CandidateStage.INTERVIEW)));

            assertThatThrownBy(() -> service.hire(8, new HireRequest(null, "M", null, null, null, null, null)))
                    .isInstanceOf(BusinessException.class);
            verify(employeeDao, never()).insert(any());
        }

        @Test
        void aSalaryOutsideTheBandStopsTheHireBeforeAnythingIsWritten() {
            when(recruitmentDao.findCandidate(8)).thenReturn(Optional.of(candidate(8, CandidateStage.OFFER)));
            when(recruitmentDao.findJob(3)).thenReturn(Optional.of(job(3, JobStatus.OPEN, 1, 0)));
            when(employeeDao.findById(11)).thenReturn(Optional.of(Fixtures.employeeRecord(11, "EMP0011")));

            assertThatThrownBy(() -> service.hire(8, new HireRequest("EMP0038", "F", null, null, vnd(50_000_000), null, null)))
                    .isInstanceOf(ValidationException.class);
            verify(recruitmentDao, never()).changeStage(anyLong(), any(), any(), any());
            assertThat(tx.rolledBack()).isEqualTo(1);
        }
    }

    @Nested
    class Candidates {

        @Test
        void applicationsAreOnlyAcceptedForOpenPostings() {
            when(recruitmentDao.findJob(3)).thenReturn(Optional.of(job(3, JobStatus.ON_HOLD, 2, 0)));

            assertThatThrownBy(() -> service.addCandidate(candidateForm("LINKEDIN", null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Tạm dừng");
        }

        @Test
        void resumeLinksMustBeHttpToKeepScriptsOut() {
            assertThatThrownBy(() -> service.addCandidate(candidateForm("WEBSITE", "javascript:alert(1)")))
                    .isInstanceOfSatisfying(ValidationException.class,
                            e -> assertThat(e.getFieldErrors()).containsKey("resumeUrl"));
        }

        @Test
        void sourceIsNormalisedIndependentlyOfTheServerLocale() {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            when(recruitmentDao.findJob(3)).thenReturn(Optional.of(job(3, JobStatus.OPEN, 2, 0)));
            when(recruitmentDao.insertCandidate(any())).thenReturn(20L);
            when(recruitmentDao.findCandidate(20)).thenReturn(Optional.of(candidate(20, CandidateStage.APPLIED)));

            service.addCandidate(candidateForm("linkedin", "https://cv.example.com/khoa.pdf"));

            ArgumentCaptor<CandidateForm> stored = ArgumentCaptor.forClass(CandidateForm.class);
            verify(recruitmentDao).insertCandidate(stored.capture());
            assertThat(stored.getValue().source()).isEqualTo("LINKEDIN");
            assertThat(stored.getValue().email()).isEqualTo("khoa.tm@example.com");
            assertThat(stored.getValue().appliedDate()).isEqualTo(Fixtures.TODAY);
        }

        @Test
        void managersMayOnlyRateAndAnnotateCandidatesOfTheirDepartments() {
            when(recruitmentDao.findCandidate(8)).thenReturn(Optional.of(candidate(8, CandidateStage.INTERVIEW)));
            when(recruitmentDao.isJobInManagerScope(3, 11)).thenReturn(true);
            when(recruitmentDao.isJobInManagerScope(3, 12)).thenReturn(false);
            CandidateForm review = new CandidateForm(3L, "Đổi tên", "x@example.com", null, null, null, null, 4, null,
                    "Giao tiếp tốt", null);

            service.updateCandidate(Fixtures.manager(11), 8, review);

            verify(recruitmentDao).updateCandidateReview(8, 4, null, "Giao tiếp tốt");
            verify(recruitmentDao, never()).updateCandidate(anyLong(), any());
            assertThatThrownBy(() -> service.updateCandidate(Fixtures.manager(12), 8, review))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void hiredCandidatesCannotBeDeleted() {
            when(recruitmentDao.findCandidate(8)).thenReturn(Optional.of(candidate(8, CandidateStage.HIRED)));

            assertThatThrownBy(() -> service.deleteCandidate(8)).isInstanceOf(BusinessException.class);
        }

        @Test
        void employeesHaveNoAccessToRecruitment() {
            assertThatThrownBy(() -> service.searchCandidates(Fixtures.employee(16), null, null, null, null))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    class Postings {

        private JobPostingForm form(String code, Integer openings, String type, long min, long max) {
            return new JobPostingForm(code, "Lập trình viên Java", 4L, 6L, openings, type, "Hà Nội", vnd(min), vnd(max),
                    null, null, null, null, null);
        }

        @Test
        void createsADraftWithNormalisedValues() {
            when(recruitmentDao.insertJob(any(), eq("hr"))).thenReturn(3L);
            when(recruitmentDao.findJob(3)).thenReturn(Optional.of(job(3, JobStatus.DRAFT, 2, 0)));

            service.createJob(Fixtures.hr(5), form(" job-dev-01 ", null, "full_time", 15_000_000, 25_000_000));

            ArgumentCaptor<JobPostingForm> stored = ArgumentCaptor.forClass(JobPostingForm.class);
            verify(recruitmentDao).insertJob(stored.capture(), eq("hr"));
            assertThat(stored.getValue().jobCode()).isEqualTo("JOB-DEV-01");
            assertThat(stored.getValue().openings()).isEqualTo(1);
            assertThat(stored.getValue().employmentType()).isEqualTo("FULL_TIME");
            assertThat(stored.getValue().status()).isEqualTo(JobStatus.DRAFT);
        }

        @Test
        void invalidPostingsReportEveryField() {
            assertThatThrownBy(() -> service.createJob(Fixtures.hr(5), form("x", 0, "SEASONAL", 25_000_000, 15_000_000)))
                    .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getFieldErrors())
                            .containsKeys("jobCode", "openings", "employmentType", "salaryMax"));
        }

        @Test
        void openingsCannotDropBelowPeopleAlreadyHired() {
            when(recruitmentDao.findJob(3)).thenReturn(Optional.of(job(3, JobStatus.OPEN, 3, 2)));

            assertThatThrownBy(() -> service.updateJob(3, form("JOB-DEV-01", 1, "FULL_TIME", 15_000_000, 25_000_000)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("(2)");
        }

        @Test
        void closedPostingsStayClosedAndPostingsWithCandidatesAreNotDeleted() {
            when(recruitmentDao.findJob(3)).thenReturn(Optional.of(job(3, JobStatus.CLOSED, 1, 1)));
            JobPosting withCandidates = job(4, JobStatus.OPEN, 1, 0);
            withCandidates.setCandidateCount(5);
            when(recruitmentDao.findJob(4)).thenReturn(Optional.of(withCandidates));

            assertThatThrownBy(() -> service.changeJobStatus(3, JobStatus.OPEN)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> service.deleteJob(4)).hasMessageContaining("5 ứng viên");
        }

        @Test
        void openingAPostingStampsTheOpenDate() {
            when(recruitmentDao.findJob(3)).thenReturn(Optional.of(job(3, JobStatus.DRAFT, 1, 0)));

            service.changeJobStatus(3, JobStatus.OPEN);

            verify(recruitmentDao).updateJobStatus(3, JobStatus.OPEN, Fixtures.TODAY, null);
        }

        @Test
        void managersSeeOnlyPostingsOfTheirDepartments() {
            when(recruitmentDao.findJob(3)).thenReturn(Optional.of(job(3, JobStatus.OPEN, 1, 0)));
            when(recruitmentDao.isJobInManagerScope(3, 12)).thenReturn(false);

            assertThatThrownBy(() -> service.job(Fixtures.manager(12), 3)).isInstanceOf(ForbiddenException.class);
            service.searchJobs(Fixtures.manager(12), null, null, null, null);
            verify(recruitmentDao).searchJobs(null, null, null, 12L, null);
        }
    }
}
