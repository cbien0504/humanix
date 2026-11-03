package com.hrm.service;

import com.hrm.dao.AttendanceDao;
import com.hrm.dao.LeaveDao;
import com.hrm.dao.PayrollDao;
import com.hrm.dao.PerformanceDao;
import com.hrm.dao.RecruitmentDao;
import com.hrm.dao.ReportDao;
import com.hrm.model.CycleStatus;
import com.hrm.model.LeaveFilter;
import com.hrm.model.LeaveStatus;
import com.hrm.model.PageRequest;
import com.hrm.model.ReviewStatus;
import com.hrm.security.AuthUser;
import com.hrm.security.Role;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardService {

    private static final PageRequest FIRST_PAGE = new PageRequest(1, 50, null, true);

    private final ReportDao reportDao;
    private final AttendanceDao attendanceDao;
    private final LeaveDao leaveDao;
    private final PayrollDao payrollDao;
    private final PerformanceDao performanceDao;
    private final RecruitmentDao recruitmentDao;
    private final Clock clock;

    public DashboardService(ReportDao reportDao, AttendanceDao attendanceDao, LeaveDao leaveDao, PayrollDao payrollDao,
                            PerformanceDao performanceDao, RecruitmentDao recruitmentDao, Clock clock) {
        this.reportDao = reportDao;
        this.attendanceDao = attendanceDao;
        this.leaveDao = leaveDao;
        this.payrollDao = payrollDao;
        this.performanceDao = performanceDao;
        this.recruitmentDao = recruitmentDao;
        this.clock = clock;
    }

    public Map<String, Object> dashboard(AuthUser user) {
        LocalDate today = LocalDate.now(clock);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("today", today);
        if (user.getEmployeeId() != null) {
            m.put("me", personal(user.getEmployeeId(), today));
        }
        if (user.hasRole(Role.MANAGER) && user.getEmployeeId() != null) {
            m.put("team", team(user.getEmployeeId(), today));
        }
        if (user.isHrOrAdmin()) {
            m.put("company", company(today));
        }
        return m;
    }

    private Map<String, Object> personal(long employeeId, LocalDate today) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("attendance", attendanceDao.find(employeeId, today).orElse(null));
        m.put("onLeave", leaveDao.hasApprovedLeaveOn(employeeId, today));
        leaveDao.balances(employeeId, today.getYear()).stream().filter(b -> "ANNUAL".equals(b.leaveTypeCode())).findFirst()
                .ifPresent(b -> m.put("annualLeaveAvailable", b.getAvailableDays()));
        payrollDao.payslipsOfEmployee(employeeId).stream().findFirst().ifPresent(p -> {
            Map<String, Object> slip = new LinkedHashMap<>();
            slip.put("payrollId", p.getPayrollId());
            slip.put("period", p.getPeriodLabel());
            slip.put("netSalary", p.getNetSalary());
            m.put("lastPayslip", slip);
        });
        m.put("pendingLeaveRequests", leaveDao.search(new LeaveFilter(null, LeaveStatus.PENDING, null, null, employeeId, null),
                FIRST_PAGE).totalItems());
        m.put("pendingSelfReviews", performanceDao.searchReviews(null, ReviewStatus.PENDING_SELF, null, employeeId, null, FIRST_PAGE)
                .items().stream().filter(r -> r.getCycleStatus() == CycleStatus.OPEN).count());
        return m;
    }

    private Map<String, Object> team(long managerId, LocalDate today) {
        Map<String, Object> m = new LinkedHashMap<>(reportDao.teamSummary(managerId, today));
        m.put("pendingApprovals", leaveDao.countPending(managerId));
        m.put("reviewsToScore", performanceDao.searchReviews(null, ReviewStatus.PENDING_MANAGER, null, null, managerId, FIRST_PAGE)
                .items().stream().filter(r -> r.getCycleStatus() == CycleStatus.OPEN).count());
        return m;
    }

    private Map<String, Object> company(LocalDate today) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("headcount", reportDao.headcountSummary(today));
        m.put("attendanceToday", reportDao.attendanceOn(today));
        m.put("pendingLeaveRequests", leaveDao.countPending(null));
        m.put("recruitment", recruitmentDao.recruitmentStats());
        YearMonth current = YearMonth.from(today);
        List<Map<String, Object>> payroll = reportDao.payrollTrend(7).stream()
                .filter(p -> YearMonth.of((Integer) p.get("year"), (Integer) p.get("month")).isBefore(current)
                        || "APPROVED".equals(p.get("status")) || "PAID".equals(p.get("status")))
                .toList();
        m.put("payrollTrend", payroll.size() > 6 ? payroll.subList(payroll.size() - 6, payroll.size()) : payroll);
        m.put("departments", reportDao.departmentStatistics(today).stream()
                .sorted(Comparator.comparingInt(s -> -s.headcount()))
                .map(s -> Map.of("name", s.departmentName(), "headcount", s.headcount()))
                .toList());
        List<Map<String, Object>> trend = reportDao.attendanceTrend(today.minusDays(28), today.minusDays(1));
        m.put("attendanceTrend", trend.size() > 14 ? trend.subList(trend.size() - 14, trend.size()) : trend);
        return m;
    }
}
