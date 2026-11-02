package com.hrm.service;

import com.hrm.dao.DepartmentDao;
import com.hrm.dao.EmployeeDao;
import com.hrm.dao.ReportDao;
import com.hrm.exception.BusinessException;
import com.hrm.exception.ForbiddenException;
import com.hrm.model.AttendanceReportRow;
import com.hrm.model.Department;
import com.hrm.model.DepartmentStat;
import com.hrm.model.Employee;
import com.hrm.model.EmployeeFilter;
import com.hrm.report.ReportColumn;
import com.hrm.report.TabularReport;
import com.hrm.security.AuthUser;
import com.hrm.security.Role;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * HR reports (UC-61, UC-62) backed by stored procedures, plus their Excel/CSV versions.
 * Managers only see the departments they run (or work in).
 */
public class ReportService {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReportDao reportDao;
    private final DepartmentDao departmentDao;
    private final EmployeeDao employeeDao;
    private final AuditService audit;
    private final Clock clock;

    public ReportService(ReportDao reportDao, DepartmentDao departmentDao, EmployeeDao employeeDao, AuditService audit,
                         Clock clock) {
        this.reportDao = reportDao;
        this.departmentDao = departmentDao;
        this.employeeDao = employeeDao;
        this.audit = audit;
        this.clock = clock;
    }

    public List<DepartmentStat> departmentStatistics(AuthUser user, LocalDate asOf) {
        LocalDate date = asOf == null ? LocalDate.now(clock) : asOf;
        List<DepartmentStat> all = reportDao.departmentStatistics(date);
        if (user.isHrOrAdmin()) {
            return all;
        }
        Set<Long> scope = scope(user);
        return all.stream().filter(s -> scope.contains(s.departmentId())).toList();
    }

    public List<AttendanceReportRow> attendanceReport(AuthUser user, LocalDate from, LocalDate to, Long departmentId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = from == null ? today.withDayOfMonth(1) : from;
        LocalDate end = to == null ? today : to;
        if (end.isBefore(start)) {
            throw new BusinessException("Ngày kết thúc phải sau ngày bắt đầu.");
        }
        if (ChronoUnit.DAYS.between(start, end) > 366) {
            throw new BusinessException("Khoảng thời gian báo cáo tối đa là 1 năm.");
        }
        if (user.isHrOrAdmin()) {
            return reportDao.attendanceReport(start, end, departmentId);
        }
        Set<Long> scope = scope(user);
        if (departmentId != null && !scope.contains(departmentId)) {
            throw new ForbiddenException("Bạn chỉ xem được báo cáo của phòng ban mình quản lý.");
        }
        return reportDao.attendanceReport(start, end, departmentId).stream()
                .filter(r -> scope.contains(r.departmentId())).toList();
    }

    public TabularReport departmentReport(AuthUser user, LocalDate asOf) {
        LocalDate date = asOf == null ? LocalDate.now(clock) : asOf;
        TabularReport r = new TabularReport("THỐNG KÊ NHÂN SỰ THEO PHÒNG BAN", List.of("Tính đến ngày " + DMY.format(date)),
                List.of(ReportColumn.text("Mã", 8), ReportColumn.text("Phòng ban", 28), ReportColumn.text("Trưởng phòng", 22),
                        ReportColumn.integer("Tổng NV").withTotal(), ReportColumn.integer("Chính thức").withTotal(),
                        ReportColumn.integer("Thử việc").withTotal(), ReportColumn.integer("Nghỉ dài hạn").withTotal(),
                        ReportColumn.integer("Nam").withTotal(), ReportColumn.integer("Nữ").withTotal(),
                        ReportColumn.money("Lương CB bình quân").noTotal(), ReportColumn.money("Quỹ lương/tháng"),
                        ReportColumn.decimal("Thâm niên TB (năm)"), ReportColumn.decimal("Tuổi TB"),
                        ReportColumn.integer("Tuyển mới (năm)").withTotal(), ReportColumn.integer("Nghỉ việc (năm)").withTotal(),
                        ReportColumn.percent("Tỷ lệ nghỉ việc")));
        for (DepartmentStat s : departmentStatistics(user, date)) {
            r.addRow(s.departmentCode(), s.departmentName(), s.managerName(), s.headcount(), s.activeCount(), s.probationCount(),
                    s.onLeaveCount(), s.maleCount(), s.femaleCount(), s.avgBaseSalary(), s.monthlySalaryFund(),
                    s.avgYearsOfService(), s.avgAge(), s.newHiresYtd(), s.terminationsYtd(), s.turnoverRate());
        }
        audit.record("REPORT_EXPORTED", "Report", "departments", DMY.format(date));
        return r;
    }

    public TabularReport attendanceReportTable(AuthUser user, LocalDate from, LocalDate to, Long departmentId) {
        List<AttendanceReportRow> rows = attendanceReport(user, from, to, departmentId);
        LocalDate start = from == null ? LocalDate.now(clock).withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now(clock) : to;
        String scope = departmentId == null ? "Tất cả phòng ban"
                : departmentDao.findById(departmentId).map(Department::getDepartmentName).orElse("#" + departmentId);
        TabularReport r = new TabularReport("BÁO CÁO CHẤM CÔNG", List.of("Từ " + DMY.format(start) + " đến " + DMY.format(end)
                + " · " + scope, "Ngày công kỳ vọng = ngày làm việc (trừ T7, CN, lễ) trong thời gian làm việc của nhân viên."),
                List.of(ReportColumn.text("Mã NV", 10), ReportColumn.text("Họ tên", 24), ReportColumn.text("Phòng ban", 24),
                        ReportColumn.integer("Công kỳ vọng"), ReportColumn.integer("Đúng giờ").withTotal(),
                        ReportColumn.integer("Đi muộn").withTotal(), ReportColumn.integer("Nửa ngày").withTotal(),
                        ReportColumn.integer("Nghỉ có lương").withTotal(), ReportColumn.integer("Nghỉ không lương").withTotal(),
                        ReportColumn.integer("Vắng").withTotal(), ReportColumn.decimal("Ngày công"),
                        ReportColumn.integer("Phút đi muộn").withTotal(), ReportColumn.decimal("Giờ làm"),
                        ReportColumn.percent("Tỷ lệ chuyên cần")));
        for (AttendanceReportRow a : rows) {
            r.addRow(a.employeeCode(), a.fullName(), a.departmentName(), a.expectedDays(), a.presentDays(), a.lateDays(),
                    a.halfDays(), a.paidLeaveDays(), a.unpaidLeaveDays(), a.absentDays(), a.workedDays(), a.lateMinutes(),
                    a.workHours(), a.attendanceRate());
        }
        audit.record("REPORT_EXPORTED", "Report", "attendance", start + "→" + end);
        return r;
    }

    /** HR: employee list with the filters of the employee screen. */
    public TabularReport employeeList(EmployeeFilter filter) {
        TabularReport r = new TabularReport("DANH SÁCH NHÂN VIÊN", List.of("Xuất ngày " + DMY.format(LocalDate.now(clock))),
                List.of(ReportColumn.text("Mã NV", 10), ReportColumn.text("Họ tên", 24), ReportColumn.text("Giới tính", 9),
                        ReportColumn.date("Ngày sinh"), ReportColumn.text("Email", 26), ReportColumn.text("Điện thoại", 13),
                        ReportColumn.text("Phòng ban", 24), ReportColumn.text("Chức danh", 22), ReportColumn.text("Quản lý", 22),
                        ReportColumn.date("Ngày vào làm"), ReportColumn.text("Trạng thái", 13), ReportColumn.money("Lương CB"),
                        ReportColumn.money("Phụ cấp"), ReportColumn.integer("Người phụ thuộc")));
        for (Employee e : employeeDao.findAll(filter)) {
            r.addRow(e.getEmployeeCode(), e.getFullName(), e.getGenderLabel(), e.getDateOfBirth(), e.getEmail(), e.getPhone(),
                    e.getDepartmentName(), e.getPositionName(), e.getManagerName(), e.getHireDate(), e.getStatus().getLabel(),
                    e.getBaseSalary(), e.getAllowance(), e.getDependents());
        }
        audit.record("REPORT_EXPORTED", "Report", "employees", r.rows().size() + " dòng");
        return r;
    }

    /** Departments a manager runs, plus the one he/she works in. */
    Set<Long> scope(AuthUser user) {
        if (!user.hasRole(Role.MANAGER) || user.getEmployeeId() == null) {
            throw new ForbiddenException();
        }
        Set<Long> ids = new HashSet<>();
        for (Department d : departmentDao.findAll()) {
            if (Objects.equals(d.getManagerId(), user.getEmployeeId())) {
                ids.add(d.getDepartmentId());
            }
        }
        employeeDao.findById(user.getEmployeeId()).ifPresent(e -> ids.add(e.getDepartmentId()));
        return ids;
    }
}
