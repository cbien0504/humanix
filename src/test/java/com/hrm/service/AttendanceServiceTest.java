package com.hrm.service;

import com.hrm.dao.AttendanceDao;
import com.hrm.dao.EmployeeDao;
import com.hrm.dao.LeaveDao;
import com.hrm.exception.BusinessException;
import com.hrm.exception.ForbiddenException;
import com.hrm.exception.NotFoundException;
import com.hrm.exception.ValidationException;
import com.hrm.model.AttendanceFilter;
import com.hrm.model.AttendanceForm;
import com.hrm.model.AttendanceRecord;
import com.hrm.model.AttendanceStatus;
import com.hrm.model.Employee;
import com.hrm.service.AttendanceService.BulkResult;
import com.hrm.testsupport.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Check-in/out (server clock), HR corrections and the time-clock bulk upload (UC-20 … UC-22, UC-71). */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    private static final LocalDate TODAY = Fixtures.TODAY;
    private static final LocalDate THU_29_OCT = LocalDate.of(2026, 10, 29);

    @Mock
    private AttendanceDao attendanceDao;
    @Mock
    private EmployeeDao employeeDao;
    @Mock
    private LeaveDao leaveDao;
    @Mock
    private AuditService audit;

    private AttendanceService serviceAt(LocalTime time) {
        Clock clock = Fixtures.clockAt(TODAY.atTime(time));
        return new AttendanceService(attendanceDao, employeeDao, leaveDao,
                new AttendancePolicy(LocalTime.of(8, 30), 5, 4), audit, clock);
    }

    private static AttendanceRecord record(long id, long employeeId, LocalDate day, LocalTime checkIn) {
        AttendanceRecord r = new AttendanceRecord();
        r.setAttendanceId(id);
        r.setEmployeeId(employeeId);
        r.setEmployeeCode("EMP00" + employeeId);
        r.setWorkDate(day);
        r.setCheckIn(checkIn == null ? null : day.atTime(checkIn));
        r.setStatus(checkIn == null ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT);
        r.setSource("WEB");
        return r;
    }

    @Nested
    class CheckInAndOut {

        @Test
        void firstCheckInUsesTheServerClockAndCountsLateMinutes() {
            AttendanceService service = serviceAt(LocalTime.of(8, 47, 30));
            when(attendanceDao.find(16, TODAY)).thenReturn(Optional.empty(),
                    Optional.of(record(1, 16, TODAY, LocalTime.of(8, 47, 30))));
            when(leaveDao.hasApprovedLeaveOn(16, TODAY)).thenReturn(false);

            service.checkIn(Fixtures.employee(16));

            verify(attendanceDao).insert(16, TODAY, TODAY.atTime(8, 47, 30), null, AttendanceStatus.LATE, 17, "WEB", null);
        }

        @Test
        void secondCheckInOfTheDayIsRefused() {
            AttendanceService service = serviceAt(LocalTime.of(9, 0));
            when(attendanceDao.find(16, TODAY)).thenReturn(Optional.of(record(1, 16, TODAY, LocalTime.of(8, 20))));

            assertThatThrownBy(() -> service.checkIn(Fixtures.employee(16)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("08:20");
        }

        @Test
        void noCheckInDuringApprovedLeave() {
            AttendanceService service = serviceAt(LocalTime.of(8, 10));
            when(attendanceDao.find(16, TODAY)).thenReturn(Optional.empty());
            when(leaveDao.hasApprovedLeaveOn(16, TODAY)).thenReturn(true);

            assertThatThrownBy(() -> service.checkIn(Fixtures.employee(16)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("nghỉ phép");
            verify(attendanceDao, never()).insert(anyLong(), any(), any(), any(), any(), anyInt(), anyString(), any());
        }

        @Test
        void checkInCompletesARowPreparedByHr() {
            AttendanceService service = serviceAt(LocalTime.of(8, 25));
            AttendanceRecord absent = record(7, 16, TODAY, null);
            absent.setNote("Ghi chú HR");
            when(attendanceDao.find(16, TODAY)).thenReturn(Optional.of(absent), Optional.of(record(7, 16, TODAY, LocalTime.of(8, 25))));
            when(leaveDao.hasApprovedLeaveOn(16, TODAY)).thenReturn(false);

            service.checkIn(Fixtures.employee(16));

            verify(attendanceDao).update(7, TODAY.atTime(8, 25), null, AttendanceStatus.PRESENT, 0, "WEB", "Ghi chú HR");
        }

        @Test
        void checkOutDecidesTheFinalStatus() {
            AttendanceService service = serviceAt(LocalTime.of(11, 0));
            AttendanceRecord morning = record(1, 16, TODAY, LocalTime.of(8, 20));
            when(attendanceDao.find(16, TODAY)).thenReturn(Optional.of(morning));
            when(attendanceDao.findById(1)).thenReturn(Optional.of(morning));

            service.checkOut(Fixtures.employee(16));

            verify(attendanceDao).update(1, TODAY.atTime(8, 20), TODAY.atTime(11, 0), AttendanceStatus.HALF_DAY, 0, "WEB", null);
        }

        @Test
        void checkOutRequiresACheckIn() {
            AttendanceService service = serviceAt(LocalTime.of(17, 30));
            when(attendanceDao.find(16, TODAY)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.checkOut(Fixtures.employee(16)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("chưa check-in");
        }

        @Test
        void technicalAccountsCannotCheckIn() {
            assertThatThrownBy(() -> serviceAt(LocalTime.NOON).checkIn(Fixtures.admin()))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void todayWidgetShowsScheduleAndLeaveFlag() {
            AttendanceService service = serviceAt(LocalTime.of(7, 59));
            when(attendanceDao.find(16, TODAY)).thenReturn(Optional.empty());
            when(leaveDao.hasApprovedLeaveOn(16, TODAY)).thenReturn(true);

            AttendanceService.Today today = service.today(Fixtures.employee(16));

            assertThat(today.workStart()).isEqualTo("08:30");
            assertThat(today.graceMinutes()).isEqualTo(5);
            assertThat(today.onLeave()).isTrue();
            assertThat(today.record()).isNull();
        }
    }

    @Nested
    class ManualEntry {

        private AttendanceService service;

        @BeforeEach
        void createService() {                          // after MockitoExtension has injected the mocks
            service = serviceAt(LocalTime.of(10, 0));
        }

        private Employee emp17() {
            return Fixtures.employeeRecord(17, "EMP0017");
        }

        @Test
        void statusAndLateMinutesAreDerivedFromTheTimes() {
            when(employeeDao.findByCode("EMP0017")).thenReturn(Optional.of(emp17()));
            when(attendanceDao.find(17, THU_29_OCT)).thenReturn(Optional.empty());
            when(attendanceDao.insert(anyLong(), any(), any(), any(), any(), anyInt(), anyString(), any())).thenReturn(40L);
            when(attendanceDao.findById(40)).thenReturn(Optional.of(record(40, 17, THU_29_OCT, LocalTime.of(8, 50))));

            service.save(new AttendanceForm(null, " emp0017 ", THU_29_OCT, LocalTime.of(8, 50), LocalTime.of(17, 35), null, null), "MANUAL");

            verify(attendanceDao).insert(17, THU_29_OCT, THU_29_OCT.atTime(8, 50), THU_29_OCT.atTime(17, 35),
                    AttendanceStatus.LATE, 20, "MANUAL", null);
            verify(audit).record(eq("ATTENDANCE_SAVED"), eq("Attendance"), eq(40L), any());
        }

        @Test
        void savingTheSameDayAgainUpdatesTheExistingRow() {
            when(employeeDao.findByCode("EMP0017")).thenReturn(Optional.of(emp17()));
            when(attendanceDao.find(17, THU_29_OCT)).thenReturn(Optional.of(record(40, 17, THU_29_OCT, LocalTime.of(8, 50))));
            when(attendanceDao.findById(40)).thenReturn(Optional.of(record(40, 17, THU_29_OCT, LocalTime.of(8, 0))));

            service.save(new AttendanceForm(null, "EMP0017", THU_29_OCT, LocalTime.of(8, 0), LocalTime.of(17, 0), null, null), "API");

            verify(attendanceDao).update(40, THU_29_OCT.atTime(8, 0), THU_29_OCT.atTime(17, 0), AttendanceStatus.PRESENT, 0, "API", null);
            verify(attendanceDao, never()).insert(anyLong(), any(), any(), any(), any(), anyInt(), anyString(), any());
            verifyNoInteractions(audit);                     // API rows are audited once per batch
        }

        @Test
        void absentDropsAnyTimes() {
            when(employeeDao.findById(17)).thenReturn(Optional.of(emp17()));
            when(attendanceDao.find(17, THU_29_OCT)).thenReturn(Optional.empty());
            when(attendanceDao.insert(anyLong(), any(), any(), any(), any(), anyInt(), anyString(), any())).thenReturn(41L);
            when(attendanceDao.findById(41)).thenReturn(Optional.of(record(41, 17, THU_29_OCT, null)));

            service.save(new AttendanceForm(17L, null, THU_29_OCT, LocalTime.of(8, 0), null, AttendanceStatus.ABSENT, "Không phép"), "MANUAL");

            verify(attendanceDao).insert(17, THU_29_OCT, null, null, AttendanceStatus.ABSENT, 0, "MANUAL", "Không phép");
        }

        @Test
        void inputErrorsAreReportedPerField() {
            AttendanceForm bad = new AttendanceForm(null, null, TODAY.plusDays(1), LocalTime.of(18, 0), LocalTime.of(8, 0), null, null);

            assertThatThrownBy(() -> service.save(bad, "MANUAL"))
                    .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getFieldErrors())
                            .containsKeys("employeeId", "workDate", "checkOut"));
            verifyNoInteractions(employeeDao, attendanceDao);
        }

        @Test
        void aDayOutsideTheEmploymentPeriodIsRefused() {
            Employee newcomer = emp17();
            newcomer.setHireDate(TODAY);
            when(employeeDao.findByCode("EMP0017")).thenReturn(Optional.of(newcomer));

            assertThatThrownBy(() -> service.save(new AttendanceForm(null, "EMP0017", THU_29_OCT, LocalTime.of(8, 0),
                    null, null, null), "MANUAL"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ngoài thời gian làm việc");
        }

        @Test
        void unknownEmployeeCodeIsNotFound() {
            when(employeeDao.findByCode("EMP9999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.save(new AttendanceForm(null, "EMP9999", THU_29_OCT, LocalTime.of(8, 0),
                    null, null, null), "API"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Không tìm thấy nhân viên EMP9999.");
        }
    }

    @Nested
    class Bulk {

        private AttendanceService service;

        @BeforeEach
        void createService() {                          // after MockitoExtension has injected the mocks
            service = serviceAt(LocalTime.of(10, 0));
        }

        @Test
        void elementsAreIndependentAndEachGetsAResult() {
            when(employeeDao.findByCode("EMP0017")).thenReturn(Optional.of(Fixtures.employeeRecord(17, "EMP0017")));
            when(employeeDao.findByCode("EMP9999")).thenReturn(Optional.empty());
            when(attendanceDao.find(17, THU_29_OCT)).thenReturn(Optional.empty());
            when(attendanceDao.insert(anyLong(), any(), any(), any(), any(), anyInt(), anyString(), any())).thenReturn(40L);
            AttendanceRecord saved = record(40, 17, THU_29_OCT, LocalTime.of(8, 50));
            saved.setStatus(AttendanceStatus.LATE);
            when(attendanceDao.findById(40)).thenReturn(Optional.of(saved));

            BulkResult result = service.saveBatch(List.of(
                    new AttendanceForm(null, "EMP0017", THU_29_OCT, LocalTime.of(8, 50), LocalTime.of(17, 35), null, null),
                    new AttendanceForm(null, "EMP9999", THU_29_OCT, LocalTime.of(8, 0), null, null, null),
                    new AttendanceForm(null, "EMP0017", TODAY.plusDays(2), LocalTime.of(8, 0), null, null, null)));

            assertThat(result.received()).isEqualTo(3);
            assertThat(result.saved()).isEqualTo(1);
            assertThat(result.failed()).isEqualTo(2);
            assertThat(result.results()).extracting(AttendanceService.BulkItemResult::ok).containsExactly(true, false, false);
            assertThat(result.results().get(0).status()).isEqualTo("LATE");
            assertThat(result.results().get(1).message()).isEqualTo("Không tìm thấy nhân viên EMP9999.");
            assertThat(result.results().get(2).message()).contains("tương lai");
            verify(audit).record("ATTENDANCE_IMPORTED", "Attendance", null, "1/3 bản ghi");
        }

        @Test
        void emptyAndOversizedBatchesAreRejectedAsAWhole() {
            List<AttendanceForm> tooMany = new ArrayList<>(Collections.nCopies(AttendanceService.MAX_BULK + 1,
                    new AttendanceForm(null, "EMP0017", THU_29_OCT, LocalTime.of(8, 0), null, null, null)));

            assertThatThrownBy(() -> service.saveBatch(List.of())).isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> service.saveBatch(tooMany))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("1000");
            verifyNoInteractions(attendanceDao, audit);
        }
    }

    @Nested
    class Queries {

        private AttendanceService service;

        @BeforeEach
        void createService() {                          // after MockitoExtension has injected the mocks
            service = serviceAt(LocalTime.of(10, 0));
        }
        private final AttendanceFilter october = new AttendanceFilter(null, null, null,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, null);

        @Test
        void searchIsScopedByRole() {
            service.search(Fixtures.hr(5), october, null);
            service.search(Fixtures.manager(11), october, null);
            service.search(Fixtures.employee(16), october, null);

            verify(attendanceDao).search(october.scopedTo(null, null), null);
            verify(attendanceDao).search(october.scopedTo(null, 11L), null);
            verify(attendanceDao).search(october.scopedTo(16L, null), null);
        }

        @Test
        void searchRangeIsLimitedToOneYear() {
            AttendanceFilter twoYears = new AttendanceFilter(null, null, null, LocalDate.of(2025, 1, 1),
                    LocalDate.of(2026, 12, 31), null, null);

            assertThatThrownBy(() -> service.search(Fixtures.hr(5), twoYears, null)).isInstanceOf(BusinessException.class);
        }

        @Test
        void monthWithoutDataHasAnEmptySummary() {
            when(attendanceDao.monthly(16, 2026, 10)).thenReturn(Optional.empty());

            var month = service.myMonth(Fixtures.employee(16), YearMonth.of(2026, 10));

            assertThat(month.summary().presentDays()).isZero();
            assertThat(month.records()).isEmpty();
        }

        @Test
        void deletingIsAudited() {
            when(attendanceDao.findById(40)).thenReturn(Optional.of(record(40, 17, THU_29_OCT, LocalTime.of(8, 0))));

            service.delete(40);

            verify(attendanceDao).delete(40);
            verify(audit).record("ATTENDANCE_DELETED", "Attendance", 40L, "EMP0017 2026-10-29");
        }

        @Test
        void deletingAnUnknownRowIsNotFound() {
            when(attendanceDao.findById(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(404)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void countsTodaysCheckIns() {
            when(attendanceDao.countCheckedIn(TODAY)).thenReturn(30L);

            assertThat(service.countCheckedInToday()).isEqualTo(30);
        }
    }

    @Test
    void checkOutTimeMustBeAfterCheckIn() {
        AttendanceService service = serviceAt(LocalTime.of(8, 0));
        AttendanceRecord future = record(1, 16, TODAY, LocalTime.of(8, 30));
        when(attendanceDao.find(16, TODAY)).thenReturn(Optional.of(future));

        assertThatThrownBy(() -> service.checkOut(Fixtures.employee(16)))
                .isInstanceOf(BusinessException.class);
        verify(attendanceDao, never()).update(anyLong(), any(LocalDateTime.class), any(), any(), anyInt(), any(), any());
    }
}
