package com.hrm.service;

import com.hrm.dao.EmployeeDao;
import com.hrm.dao.HolidayDao;
import com.hrm.dao.LeaveDao;
import com.hrm.exception.BusinessException;
import com.hrm.exception.ForbiddenException;
import com.hrm.exception.ValidationException;
import com.hrm.model.LeaveBalance;
import com.hrm.model.LeaveFilter;
import com.hrm.model.LeaveRequest;
import com.hrm.model.LeaveRequestForm;
import com.hrm.model.LeaveStatus;
import com.hrm.model.LeaveType;
import com.hrm.security.AuthUser;
import com.hrm.security.Role;
import com.hrm.testsupport.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    private static final LocalDate FRI_6 = LocalDate.of(2026, 11, 6);
    private static final LocalDate SAT_7 = LocalDate.of(2026, 11, 7);
    private static final LocalDate SUN_8 = LocalDate.of(2026, 11, 8);
    private static final LocalDate MON_9 = LocalDate.of(2026, 11, 9);
    private static final LocalDate TUE_10 = LocalDate.of(2026, 11, 10);
    private static final LocalDate FRI_13 = LocalDate.of(2026, 11, 13);

    private static final LeaveType ANNUAL = new LeaveType(1, "ANNUAL", "Phép năm", true, true, null, true);
    private static final LeaveType SICK = new LeaveType(2, "SICK", "Nghỉ ốm", true, false, null, true);
    private static final LeaveType WEDDING = new LeaveType(4, "WEDDING", "Nghỉ cưới", true, false, BigDecimal.valueOf(3), true);

    @Mock
    private LeaveDao leaveDao;
    @Mock
    private EmployeeDao employeeDao;
    @Mock
    private HolidayDao holidayDao;
    @Mock
    private AuditService audit;

    private LeaveService service;

    @BeforeEach
    void setUp() {
        service = new LeaveService(leaveDao, employeeDao, holidayDao, audit, Fixtures.clock(), 30);
    }

    private static LeaveRequestForm form(long typeId, LocalDate start, LocalDate end) {
        return new LeaveRequestForm(typeId, start, end, "Việc gia đình");
    }

    private static LeaveBalance annualBalance(int entitled, int used, int pending) {
        return new LeaveBalance(1, "ANNUAL", "Phép năm", 2026, BigDecimal.valueOf(entitled), BigDecimal.valueOf(used),
                BigDecimal.valueOf(pending));
    }

    private static LeaveRequest request(long id, long employeeId, LeaveStatus status, LocalDate start) {
        LeaveRequest r = new LeaveRequest();
        r.setLeaveRequestId(id);
        r.setEmployeeId(employeeId);
        r.setEmployeeCode("EMP00" + employeeId);
        r.setStatus(status);
        r.setStartDate(start);
        r.setEndDate(start.plusDays(1));
        return r;
    }

    @Nested
    class Submit {

        @Test
        void storesARequestWithItsWorkingDaysAndAudits() {
            when(leaveDao.leaveType(1)).thenReturn(Optional.of(ANNUAL));
            when(holidayDao.findBetween(MON_9, FRI_13)).thenReturn(List.of());
            when(leaveDao.findOverlapping(16, MON_9, FRI_13)).thenReturn(List.of());
            when(leaveDao.balances(16, 2026)).thenReturn(List.of(annualBalance(12, 0, 0)));
            when(leaveDao.insert(eq(16L), any(LeaveRequestForm.class), eq(BigDecimal.valueOf(5)))).thenReturn(99L);
            when(leaveDao.findById(99)).thenReturn(Optional.of(request(99, 16, LeaveStatus.PENDING, MON_9)));

            LeaveRequest saved = service.submit(Fixtures.employee(16), form(1, MON_9, FRI_13));

            assertThat(saved.getStatus()).isEqualTo(LeaveStatus.PENDING);
            verify(audit).record("LEAVE_SUBMITTED", "LeaveRequest", 99L, "ANNUAL 2026-11-09→2026-11-13 (5d)");
        }

        @Test
        void weekendsAndPublicHolidaysAreNotCounted() {
            when(leaveDao.leaveType(2)).thenReturn(Optional.of(SICK));
            when(holidayDao.findBetween(FRI_6, TUE_10)).thenReturn(List.of(MON_9));
            when(leaveDao.findOverlapping(16, FRI_6, TUE_10)).thenReturn(List.of());
            when(leaveDao.insert(eq(16L), any(LeaveRequestForm.class), eq(BigDecimal.valueOf(2)))).thenReturn(7L);
            when(leaveDao.findById(7)).thenReturn(Optional.of(request(7, 16, LeaveStatus.PENDING, FRI_6)));

            service.submit(Fixtures.employee(16), form(2, FRI_6, TUE_10));

            verify(leaveDao, never()).balances(anyLong(), anyInt());
        }

        @Test
        void aRangeWithoutWorkingDaysIsRejected() {
            when(leaveDao.leaveType(1)).thenReturn(Optional.of(ANNUAL));
            when(holidayDao.findBetween(SAT_7, SUN_8)).thenReturn(List.of());

            assertThatThrownBy(() -> service.submit(Fixtures.employee(16), form(1, SAT_7, SUN_8)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("không có ngày làm việc");
        }

        @Test
        void pendingDaysAreReservedSoTheBalanceCannotBeOverdrawn() {
            when(leaveDao.leaveType(1)).thenReturn(Optional.of(ANNUAL));
            when(holidayDao.findBetween(MON_9, FRI_13)).thenReturn(List.of());
            when(leaveDao.findOverlapping(16, MON_9, FRI_13)).thenReturn(List.of());
            when(leaveDao.balances(16, 2026)).thenReturn(List.of(annualBalance(12, 8, 2)));

            assertThatThrownBy(() -> service.submit(Fixtures.employee(16), form(1, MON_9, FRI_13)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("còn 2 ngày khả dụng")
                    .hasMessageContaining("cần 5 ngày");
            verify(leaveDao, never()).insert(anyLong(), any(), any());
        }

        @Test
        void overlappingRequestsAreRejectedWithTheirDates() {
            when(leaveDao.leaveType(1)).thenReturn(Optional.of(ANNUAL));
            when(holidayDao.findBetween(MON_9, FRI_13)).thenReturn(List.of());
            when(leaveDao.findOverlapping(16, MON_9, FRI_13))
                    .thenReturn(List.of(request(5, 16, LeaveStatus.PENDING, TUE_10)));

            assertThatThrownBy(() -> service.submit(Fixtures.employee(16), form(1, MON_9, FRI_13)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Trùng thời gian")
                    .hasMessageContaining("10/11/2026–11/11/2026 (Chờ duyệt)");
        }

        @Test
        void typeLimitPerRequestIsEnforced() {
            when(leaveDao.leaveType(4)).thenReturn(Optional.of(WEDDING));
            when(holidayDao.findBetween(MON_9, FRI_13)).thenReturn(List.of());

            assertThatThrownBy(() -> service.submit(Fixtures.employee(16), form(4, MON_9, FRI_13)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("tối đa 3 ngày");
        }

        @Test
        void missingBalanceRowIsReportedClearly() {
            when(leaveDao.leaveType(1)).thenReturn(Optional.of(ANNUAL));
            when(holidayDao.findBetween(MON_9, FRI_13)).thenReturn(List.of());
            when(leaveDao.findOverlapping(16, MON_9, FRI_13)).thenReturn(List.of());
            when(leaveDao.balances(16, 2026)).thenReturn(List.of());

            assertThatThrownBy(() -> service.submit(Fixtures.employee(16), form(1, MON_9, FRI_13)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Chưa có số dư phép năm năm 2026");
        }

        @Test
        void formErrorsAreReportedBeforeAnyDatabaseAccess() {
            assertThatThrownBy(() -> service.submit(Fixtures.employee(16), new LeaveRequestForm(1L, FRI_13, MON_9, " ")))
                    .isInstanceOfSatisfying(ValidationException.class,
                            e -> assertThat(e.getFieldErrors()).containsKeys("endDate", "reason"));
            verifyNoInteractions(leaveDao, holidayDao);
        }

        @Test
        void aRequestMayNotSpanTwoYears() {
            assertThatThrownBy(() -> service.submit(Fixtures.employee(16),
                    form(1, LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2))))
                    .isInstanceOfSatisfying(ValidationException.class,
                            e -> assertThat(e.getFieldErrors()).containsKey("endDate"));
        }

        @Test
        void backdatingIsLimitedToThirtyDays() {
            assertThatThrownBy(() -> service.submit(Fixtures.employee(16),
                    form(1, LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 2))))
                    .isInstanceOfSatisfying(ValidationException.class,
                            e -> assertThat(e.getFieldErrors()).containsKey("startDate"));
        }

        @Test
        void accountsWithoutEmployeeRecordCannotRequestLeave() {
            assertThatThrownBy(() -> service.submit(Fixtures.admin(), form(1, MON_9, FRI_13)))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    class Approval {

        @Test
        void managerApprovesARequestOfHisTeam() {
            when(leaveDao.findById(50)).thenReturn(Optional.of(request(50, 16, LeaveStatus.PENDING, MON_9)));
            when(employeeDao.isInTeam(11, 16)).thenReturn(true);
            when(leaveDao.changeStatus(50, LeaveStatus.PENDING, LeaveStatus.APPROVED, 11L, "OK")).thenReturn(1);

            service.approve(Fixtures.manager(11), 50, "  OK ");

            verify(audit).record(eq("LEAVE_APPROVED"), eq("LeaveRequest"), eq(50L), any());
        }

        @Test
        void hrApprovesWithoutTeamCheck() {
            when(leaveDao.findById(50)).thenReturn(Optional.of(request(50, 16, LeaveStatus.PENDING, MON_9)));
            when(leaveDao.changeStatus(50, LeaveStatus.PENDING, LeaveStatus.APPROVED, 5L, null)).thenReturn(1);

            service.approve(Fixtures.hr(5), 50, null);

            verify(employeeDao, never()).isInTeam(anyLong(), anyLong());
        }

        @Test
        void nobodyApprovesHisOwnRequest() {
            when(leaveDao.findById(50)).thenReturn(Optional.of(request(50, 16, LeaveStatus.PENDING, MON_9)));

            assertThatThrownBy(() -> service.approve(Fixtures.manager(16), 50, null))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("tự duyệt");
        }

        @Test
        void aManagerOfAnotherTeamCannotDecide() {
            when(leaveDao.findById(50)).thenReturn(Optional.of(request(50, 16, LeaveStatus.PENDING, MON_9)));
            when(employeeDao.isInTeam(12, 16)).thenReturn(false);

            assertThatThrownBy(() -> service.approve(Fixtures.manager(12), 50, null))
                    .isInstanceOf(ForbiddenException.class);
            verify(leaveDao, never()).changeStatus(anyLong(), any(), any(), any(), any());
        }

        @Test
        void approverMustBeLinkedToAnEmployee() {
            when(leaveDao.findById(50)).thenReturn(Optional.of(request(50, 16, LeaveStatus.PENDING, MON_9)));

            assertThatThrownBy(() -> service.approve(Fixtures.admin(), 50, null)).isInstanceOf(ForbiddenException.class);
        }

        @Test
        void aDecidedRequestCannotBeDecidedAgain() {
            when(leaveDao.findById(50)).thenReturn(Optional.of(request(50, 16, LeaveStatus.APPROVED, MON_9)));

            assertThatThrownBy(() -> service.approve(Fixtures.hr(5), 50, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Đã duyệt");
        }

        @Test
        void aConcurrentDecisionIsDetected() {
            when(leaveDao.findById(50)).thenReturn(Optional.of(request(50, 16, LeaveStatus.PENDING, MON_9)));
            when(leaveDao.changeStatus(50, LeaveStatus.PENDING, LeaveStatus.APPROVED, 5L, null)).thenReturn(0);

            assertThatThrownBy(() -> service.approve(Fixtures.hr(5), 50, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("người khác cập nhật");
            verifyNoInteractions(audit);
        }

        @Test
        void rejectingRequiresAReason() {
            assertThatThrownBy(() -> service.reject(Fixtures.hr(5), 50, "  "))
                    .isInstanceOfSatisfying(ValidationException.class,
                            e -> assertThat(e.getFieldErrors()).containsKey("comment"));
            verifyNoInteractions(leaveDao);
        }

        @Test
        void rejectStoresTheReason() {
            when(leaveDao.findById(50)).thenReturn(Optional.of(request(50, 16, LeaveStatus.PENDING, MON_9)));
            when(leaveDao.changeStatus(50, LeaveStatus.PENDING, LeaveStatus.REJECTED, 5L, "Dự án đang gấp")).thenReturn(1);

            service.reject(Fixtures.hr(5), 50, "Dự án đang gấp");

            verify(audit).record(eq("LEAVE_REJECTED"), eq("LeaveRequest"), eq(50L), any());
        }
    }

    @Nested
    class Cancel {

        @Test
        void ownerCancelsAPendingRequest() {
            when(leaveDao.findById(50)).thenReturn(Optional.of(request(50, 16, LeaveStatus.PENDING, MON_9)));
            when(leaveDao.changeStatus(50, LeaveStatus.PENDING, LeaveStatus.CANCELLED, null, null)).thenReturn(1);

            service.cancel(Fixtures.employee(16), 50);

            verify(audit).record("LEAVE_CANCELLED", "LeaveRequest", 50L, null);
        }

        @Test
        void anApprovedRequestCanBeCancelledOnlyBeforeItStarts() {
            when(leaveDao.findById(50)).thenReturn(Optional.of(request(50, 16, LeaveStatus.APPROVED, MON_9)));
            when(leaveDao.changeStatus(50, LeaveStatus.APPROVED, LeaveStatus.CANCELLED, null, null)).thenReturn(1);
            service.cancel(Fixtures.employee(16), 50);

            when(leaveDao.findById(51)).thenReturn(Optional.of(request(51, 16, LeaveStatus.APPROVED, Fixtures.TODAY)));
            assertThatThrownBy(() -> service.cancel(Fixtures.employee(16), 51)).isInstanceOf(BusinessException.class);
        }

        @Test
        void nobodyCancelsSomeoneElsesRequest() {
            when(leaveDao.findById(50)).thenReturn(Optional.of(request(50, 16, LeaveStatus.PENDING, MON_9)));

            assertThatThrownBy(() -> service.cancel(Fixtures.employee(17), 50)).isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    class Scope {

        private final LeaveFilter pending = new LeaveFilter(null, LeaveStatus.PENDING, null, null, null, null);

        @Test
        void hrSeesEveryRequestAndManagersOnlyTheirTeam() {
            service.approvalQueue(Fixtures.hr(5), pending, null);
            service.approvalQueue(Fixtures.manager(11), pending, null);

            verify(leaveDao).search(pending, null);
            verify(leaveDao).search(new LeaveFilter(null, LeaveStatus.PENDING, null, null, null, 11L), null);
        }

        @Test
        void employeesHaveNoApprovalQueue() {
            assertThatThrownBy(() -> service.approvalQueue(Fixtures.employee(16), pending, null))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void myRequestsAreAlwaysLimitedToMe() {
            service.myRequests(Fixtures.employee(16), new LeaveFilter(null, null, null, null, 99L, null), null);

            verify(leaveDao).search(new LeaveFilter(null, null, null, null, 16L, null), null);
        }

        @Test
        void pendingCounterFollowsTheRole() {
            when(leaveDao.countPending(null)).thenReturn(7L);
            when(leaveDao.countPending(11L)).thenReturn(2L);
            AuthUser unlinked = new AuthUser(60, "ketoan", null, "Kế toán", EnumSet.of(Role.EMPLOYEE));

            assertThat(service.countPending(Fixtures.hr(5))).isEqualTo(7);
            assertThat(service.countPending(Fixtures.manager(11))).isEqualTo(2);
            assertThat(service.countPending(unlinked)).isZero();
        }
    }

    @Test
    void workingDaysPreviewIsZeroForAnInvertedRange() {
        assertThat(service.workingDays(FRI_13, MON_9)).isZero();
        verifyNoInteractions(holidayDao);
    }

    @Test
    void initialisingBalancesIsAudited() {
        when(leaveDao.initBalances(2027)).thenReturn(36);

        assertThat(service.initBalances(2027)).isEqualTo(36);
        verify(audit).record("LEAVE_BALANCES_INITIALISED", "LeaveBalance", 2027, "36 dòng");
    }
}
