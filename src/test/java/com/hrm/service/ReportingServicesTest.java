package com.hrm.service;

import com.hrm.dao.AttendanceDao;
import com.hrm.dao.DepartmentDao;
import com.hrm.dao.EmployeeDao;
import com.hrm.dao.LeaveDao;
import com.hrm.dao.PayrollDao;
import com.hrm.dao.PerformanceDao;
import com.hrm.dao.RecruitmentDao;
import com.hrm.dao.ReportDao;
import com.hrm.exception.BusinessException;
import com.hrm.exception.ForbiddenException;
import com.hrm.model.AttendanceReportRow;
import com.hrm.model.CycleStatus;
import com.hrm.model.Department;
import com.hrm.model.DepartmentStat;
import com.hrm.model.Employee;
import com.hrm.model.EmployeeFilter;
import com.hrm.model.LeaveBalance;
import com.hrm.model.Page;
import com.hrm.model.PerformanceReview;
import com.hrm.model.Payslip;
import com.hrm.model.ReviewStatus;
import com.hrm.report.TabularReport;
import com.hrm.testsupport.Fixtures;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.hrm.testsupport.Fixtures.vnd;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServicesTest {

    private static final LocalDate TODAY = Fixtures.TODAY;

    @Mock
    private ReportDao reportDao;
    @Mock
    private DepartmentDao departmentDao;
    @Mock
    private EmployeeDao employeeDao;
    @Mock
    private AttendanceDao attendanceDao;
    @Mock
    private LeaveDao leaveDao;
    @Mock
    private PayrollDao payrollDao;
    @Mock
    private PerformanceDao performanceDao;
    @Mock
    private RecruitmentDao recruitmentDao;
    @Mock
    private AuditService audit;

    private static DepartmentStat stat(long id, String code, int headcount) {
        return new DepartmentStat(id, code, "Phòng " + code, "Trưởng phòng", headcount, headcount, 0, 0, headcount, 0,
                vnd(20_000_000), vnd(20_000_000L * headcount), BigDecimal.ONE, BigDecimal.valueOf(30), 1, 0, BigDecimal.ZERO);
    }

    private static AttendanceReportRow row(long departmentId, String code) {
        return new AttendanceReportRow(16, code, "Nhân viên " + code, departmentId, "Phòng " + departmentId, "Lập trình viên",
                21, 18, 2, 1, 0, 0, 0, BigDecimal.valueOf(20.5), 35, BigDecimal.valueOf(170), new BigDecimal("0.976"));
    }

    @Nested
    class Reports {

        private ReportService service() {
            return new ReportService(reportDao, departmentDao, employeeDao, audit, Fixtures.clock());
        }

        private void managerElevenRunsIt() {
            Department it = Fixtures.department(4, "IT", true);
            it.setManagerId(11L);
            Department sales = Fixtures.department(5, "SALES", true);
            sales.setManagerId(23L);
            when(departmentDao.findAll()).thenReturn(List.of(it, sales));
            when(employeeDao.findById(11)).thenReturn(Optional.of(Fixtures.employeeRecord(11, "EMP0011")));
        }

        @Test
        void hrSeesEveryDepartmentAndManagersOnlyTheirOwn() {
            when(reportDao.departmentStatistics(TODAY)).thenReturn(List.of(stat(4, "IT", 11), stat(5, "SALES", 8)));
            managerElevenRunsIt();

            assertThat(service().departmentStatistics(Fixtures.hr(5), null)).hasSize(2);
            assertThat(service().departmentStatistics(Fixtures.manager(11), null))
                    .extracting(DepartmentStat::departmentCode).containsExactly("IT");
            assertThatThrownBy(() -> service().departmentStatistics(Fixtures.employee(16), null))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void attendanceReportDefaultsToTheCurrentMonthAndChecksTheRange() {
            service().attendanceReport(Fixtures.hr(5), null, null, null);

            verify(reportDao).attendanceReport(LocalDate.of(2026, 11, 1), TODAY, null);
            assertThatThrownBy(() -> service().attendanceReport(Fixtures.hr(5), TODAY, TODAY.minusDays(1), null))
                    .isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> service().attendanceReport(Fixtures.hr(5), TODAY.minusYears(2), TODAY, null))
                    .hasMessageContaining("1 năm");
        }

        @Test
        void managersGetOnlyRowsOfTheirDepartments() {
            managerElevenRunsIt();
            when(reportDao.attendanceReport(any(), any(), isNull())).thenReturn(List.of(row(4, "EMP0016"), row(5, "EMP0024")));

            assertThat(service().attendanceReport(Fixtures.manager(11), null, null, null))
                    .extracting(AttendanceReportRow::employeeCode).containsExactly("EMP0016");
            assertThatThrownBy(() -> service().attendanceReport(Fixtures.manager(11), null, null, 5L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void exportsHaveTitlesColumnsAndAnAuditEntry() {
            when(reportDao.departmentStatistics(TODAY)).thenReturn(List.of(stat(4, "IT", 11)));
            when(reportDao.attendanceReport(any(), any(), eq(4L))).thenReturn(List.of(row(4, "EMP0016")));
            when(departmentDao.findById(4)).thenReturn(Optional.of(Fixtures.department(4, "IT", true)));
            Employee e = Fixtures.employeeRecord(16, "EMP0016");
            when(employeeDao.findAll(any())).thenReturn(List.of(e));

            TabularReport departments = service().departmentReport(Fixtures.hr(5), null);
            TabularReport attendance = service().attendanceReportTable(Fixtures.hr(5), null, null, 4L);
            TabularReport employees = service().employeeList(new EmployeeFilter(null, 4L, null, null, null));

            assertThat(departments.rows()).hasSize(1);
            assertThat(departments.rows().get(0)).hasSameSizeAs(departments.columns());
            assertThat(attendance.infoLines().get(0)).isEqualTo("Từ 01/11/2026 đến 02/11/2026 · Phòng IT");
            assertThat(employees.rows().get(0).get(0)).isEqualTo("EMP0016");
            verify(audit).record("REPORT_EXPORTED", "Report", "employees", "1 dòng");
            verify(audit).record(eq("REPORT_EXPORTED"), eq("Report"), eq("departments"), anyString());
            verify(audit).record(eq("REPORT_EXPORTED"), eq("Report"), eq("attendance"), anyString());
        }
    }

    @Nested
    class Dashboard {

        private DashboardService service() {
            return new DashboardService(reportDao, attendanceDao, leaveDao, payrollDao, performanceDao, recruitmentDao,
                    Fixtures.clock());
        }

        private PerformanceReview reviewIn(CycleStatus cycle) {
            PerformanceReview r = new PerformanceReview();
            r.setCycleStatus(cycle);
            return r;
        }

        private void personalData(long employeeId) {
            when(attendanceDao.find(employeeId, TODAY)).thenReturn(Optional.empty());
            when(leaveDao.hasApprovedLeaveOn(employeeId, TODAY)).thenReturn(false);
            when(leaveDao.balances(employeeId, 2026)).thenReturn(List.of(new LeaveBalance(1, "ANNUAL", "Phép năm", 2026,
                    BigDecimal.valueOf(12), BigDecimal.valueOf(3), BigDecimal.valueOf(2))));
            Payslip october = new Payslip();
            october.setPayrollId(77);
            october.setPeriodYear(2026);
            october.setPeriodMonth(10);
            october.setNetSalary(vnd(18_250_000));
            when(payrollDao.payslipsOfEmployee(employeeId)).thenReturn(List.of(october));
            when(leaveDao.search(any(), any())).thenReturn(new Page<>(List.of(), 1, 50, 2));
            when(performanceDao.searchReviews(isNull(), eq(ReviewStatus.PENDING_SELF), isNull(), eq(employeeId), isNull(), any()))
                    .thenReturn(new Page<>(List.of(reviewIn(CycleStatus.OPEN), reviewIn(CycleStatus.CLOSED)), 1, 50, 2));
        }

        @Test
        @SuppressWarnings("unchecked")
        void anEmployeeSeesOnlyHisPersonalSection() {
            personalData(16);

            Map<String, Object> d = service().dashboard(Fixtures.employee(16));

            assertThat(d).containsOnlyKeys("today", "me");
            Map<String, Object> me = (Map<String, Object>) d.get("me");
            assertThat((BigDecimal) me.get("annualLeaveAvailable")).isEqualByComparingTo("7");
            assertThat(me.get("pendingLeaveRequests")).isEqualTo(2L);
            assertThat(me.get("pendingSelfReviews")).as("closed cycles do not count").isEqualTo(1L);
            assertThat((Map<String, Object>) me.get("lastPayslip")).containsEntry("period", "10/2026").containsEntry("payrollId", 77L);
        }

        @Test
        @SuppressWarnings("unchecked")
        void aManagerAlsoSeesHisTeam() {
            personalData(11);
            when(reportDao.teamSummary(11, TODAY)).thenReturn(Map.of("teamSize", 10, "checkedIn", 8));
            when(leaveDao.countPending(11L)).thenReturn(3L);
            when(performanceDao.searchReviews(isNull(), eq(ReviewStatus.PENDING_MANAGER), isNull(), isNull(), eq(11L), any()))
                    .thenReturn(new Page<>(List.of(reviewIn(CycleStatus.OPEN)), 1, 50, 1));

            Map<String, Object> team = (Map<String, Object>) service().dashboard(Fixtures.manager(11)).get("team");

            assertThat(team).containsEntry("teamSize", 10).containsEntry("pendingApprovals", 3L).containsEntry("reviewsToScore", 1L);
        }

        @Test
        @SuppressWarnings("unchecked")
        void hrGetsCompanyFiguresWithoutTheRunningPayrollMonth() {
            when(reportDao.headcountSummary(TODAY)).thenReturn(Map.of("headcount", 36));
            when(reportDao.attendanceOn(TODAY)).thenReturn(Map.of("checkedIn", 30));
            when(leaveDao.countPending(null)).thenReturn(4L);
            when(recruitmentDao.recruitmentStats()).thenReturn(Map.of("openJobs", 3L));
            when(reportDao.payrollTrend(7)).thenReturn(List.of(
                    Map.of("year", 2026, "month", 9, "status", "PAID"),
                    Map.of("year", 2026, "month", 10, "status", "CALCULATED"),
                    Map.of("year", 2026, "month", 11, "status", "CALCULATED")));
            when(reportDao.departmentStatistics(TODAY)).thenReturn(List.of(stat(5, "SALES", 8), stat(4, "IT", 11)));
            when(reportDao.attendanceTrend(TODAY.minusDays(28), TODAY.minusDays(1))).thenReturn(List.of());

            Map<String, Object> d = service().dashboard(Fixtures.admin());
            Map<String, Object> company = (Map<String, Object>) d.get("company");

            assertThat(d).doesNotContainKey("me");
            assertThat((List<Map<String, Object>>) company.get("payrollTrend")).extracting(p -> p.get("month"))
                    .containsExactly(9, 10);
            assertThat((List<Map<String, Object>>) company.get("departments")).extracting(p -> p.get("name"))
                    .containsExactly("Phòng IT", "Phòng SALES");
            verify(attendanceDao, never()).find(anyLong(), any());
        }
    }
}
