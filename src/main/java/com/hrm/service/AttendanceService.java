package com.hrm.service;

import com.hrm.dao.AttendanceDao;
import com.hrm.dao.EmployeeDao;
import com.hrm.dao.LeaveDao;
import com.hrm.exception.BusinessException;
import com.hrm.exception.ForbiddenException;
import com.hrm.exception.NotFoundException;
import com.hrm.model.AttendanceFilter;
import com.hrm.model.AttendanceForm;
import com.hrm.model.AttendanceRecord;
import com.hrm.model.AttendanceStatus;
import com.hrm.model.Employee;
import com.hrm.model.EmployeeStatus;
import com.hrm.model.MonthlyAttendance;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.security.AuthUser;
import com.hrm.security.Role;
import com.hrm.util.Validator;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Attendance use cases (UC-20 … UC-22). Check-in/out times always come from the server clock
 * (BR-10) – the client cannot choose its own time.
 */
public class AttendanceService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final AttendanceDao attendanceDao;
    private final EmployeeDao employeeDao;
    private final LeaveDao leaveDao;
    private final AttendancePolicy policy;
    private final AuditService audit;
    private final Clock clock;

    public AttendanceService(AttendanceDao attendanceDao, EmployeeDao employeeDao, LeaveDao leaveDao,
                             AttendancePolicy policy, AuditService audit, Clock clock) {
        this.attendanceDao = attendanceDao;
        this.employeeDao = employeeDao;
        this.leaveDao = leaveDao;
        this.policy = policy;
        this.audit = audit;
        this.clock = clock;
    }

    /** Today's record of the current user plus server time and schedule (for the check-in widget). */
    public record Today(LocalDateTime serverTime, String workStart, int graceMinutes, AttendanceRecord record,
                        boolean onLeave) {
    }

    /** Records and summary of one month. */
    public record MonthView(int year, int month, MonthlyAttendance summary, List<AttendanceRecord> records) {
    }

    public Today today(AuthUser user) {
        long employeeId = requireEmployee(user);
        LocalDateTime now = now();
        return new Today(now, policy.getWorkStart().format(HH_MM), policy.getGraceMinutes(),
                attendanceDao.find(employeeId, now.toLocalDate()).orElse(null),
                leaveDao.hasApprovedLeaveOn(employeeId, now.toLocalDate()));
    }

    public AttendanceRecord checkIn(AuthUser user) {
        long employeeId = requireEmployee(user);
        LocalDateTime now = now();
        LocalDate today = now.toLocalDate();
        Optional<AttendanceRecord> existing = attendanceDao.find(employeeId, today);
        if (existing.isPresent() && existing.get().getCheckIn() != null) {
            throw new BusinessException("Bạn đã check-in hôm nay lúc " + existing.get().getCheckIn().format(HH_MM) + ".");
        }
        if (leaveDao.hasApprovedLeaveOn(employeeId, today)) {
            throw new BusinessException("Hôm nay bạn đang trong thời gian nghỉ phép đã được duyệt.");
        }
        AttendancePolicy.CheckInResult result = policy.evaluateCheckIn(now.toLocalTime());
        if (existing.isPresent()) {   // e.g. HR pre-created an ABSENT row
            attendanceDao.update(existing.get().getAttendanceId(), now, null, result.status(), result.lateMinutes(), "WEB",
                    existing.get().getNote());
        } else {
            attendanceDao.insert(employeeId, today, now, null, result.status(), result.lateMinutes(), "WEB", null);
        }
        return attendanceDao.find(employeeId, today).orElseThrow();
    }

    public AttendanceRecord checkOut(AuthUser user) {
        long employeeId = requireEmployee(user);
        LocalDateTime now = now();
        AttendanceRecord record = attendanceDao.find(employeeId, now.toLocalDate())
                .filter(r -> r.getCheckIn() != null)
                .orElseThrow(() -> new BusinessException("Bạn chưa check-in hôm nay."));
        if (!now.isAfter(record.getCheckIn())) {
            throw new BusinessException("Giờ check-out phải sau giờ check-in.");
        }
        AttendanceStatus status = policy.evaluateDay(record.getCheckIn(), now);
        attendanceDao.update(record.getAttendanceId(), record.getCheckIn(), now, status, record.getLateMinutes(),
                record.getSource(), record.getNote());
        return attendanceDao.findById(record.getAttendanceId()).orElseThrow();
    }

    public MonthView myMonth(AuthUser user, YearMonth month) {
        long employeeId = requireEmployee(user);
        YearMonth m = month == null ? YearMonth.now(clock) : month;
        List<AttendanceRecord> records = attendanceDao.findByEmployee(employeeId, m.atDay(1), m.atEndOfMonth());
        MonthlyAttendance summary = attendanceDao.monthly(employeeId, m.getYear(), m.getMonthValue())
                .orElse(MonthlyAttendance.empty(m.getYear(), m.getMonthValue()));
        return new MonthView(m.getYear(), m.getMonthValue(), summary, records);
    }

    /** HR: everyone · Manager: own team · Employee: own records. */
    public Page<AttendanceRecord> search(AuthUser user, AttendanceFilter filter, PageRequest page) {
        if (filter.from() != null && filter.to() != null && ChronoUnit.DAYS.between(filter.from(), filter.to()) > 366) {
            throw new BusinessException("Khoảng thời gian tra cứu tối đa 1 năm.");
        }
        if (user.isHrOrAdmin()) {
            return attendanceDao.search(filter.scopedTo(null, null), page);
        }
        if (user.hasRole(Role.MANAGER) && user.getEmployeeId() != null) {
            return attendanceDao.search(filter.scopedTo(null, user.getEmployeeId()), page);
        }
        return attendanceDao.search(filter.scopedTo(requireEmployee(user), null), page);
    }

    /**
     * Creates or corrects the record of an employee/day (HR manual entry, time-clock import).
     * Status is derived from the times unless ABSENT is given explicitly.
     */
    public AttendanceRecord save(AttendanceForm form, String source) {
        Validator v = new Validator()
                .check(form.employeeId() != null || form.employeeCode() != null, "employeeId", "Vui lòng chọn nhân viên.")
                .required("workDate", form.workDate(), "Vui lòng nhập ngày chấm công.")
                .check(form.workDate() == null || !form.workDate().isAfter(LocalDate.now(clock)), "workDate",
                        "Không thể chấm công cho ngày trong tương lai.")
                .check(form.status() == AttendanceStatus.ABSENT || form.checkIn() != null, "checkIn",
                        "Vui lòng nhập giờ check-in (hoặc chọn trạng thái Vắng).")
                .check(form.checkOut() == null || form.checkIn() == null || form.checkOut().isAfter(form.checkIn()),
                        "checkOut", "Giờ check-out phải sau giờ check-in.")
                .maxLength("note", form.note(), 500);
        v.throwIfInvalid();
        Employee employee = (form.employeeId() != null ? employeeDao.findById(form.employeeId())
                : employeeDao.findByCode(Validator.upper(form.employeeCode())))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên "
                        + (form.employeeId() != null ? "#" + form.employeeId() : form.employeeCode()) + "."));
        if (form.workDate().isBefore(employee.getHireDate())
                || (employee.getTerminationDate() != null && form.workDate().isAfter(employee.getTerminationDate()))) {
            throw new BusinessException("Ngày " + form.workDate() + " nằm ngoài thời gian làm việc của " + employee.getEmployeeCode() + ".");
        }
        if (employee.getStatus() == EmployeeStatus.TERMINATED && employee.getTerminationDate() == null) {
            throw new BusinessException("Nhân viên đã nghỉ việc.");
        }

        LocalDateTime in = form.checkIn() == null ? null : form.workDate().atTime(form.checkIn());
        LocalDateTime out = form.checkOut() == null ? null : form.workDate().atTime(form.checkOut());
        AttendanceStatus status;
        int late;
        if (form.status() == AttendanceStatus.ABSENT || in == null) {
            status = AttendanceStatus.ABSENT;
            late = 0;
            in = null;
            out = null;
        } else {
            status = policy.evaluateDay(in, out);
            late = policy.evaluateCheckIn(in.toLocalTime()).lateMinutes();
        }
        Optional<AttendanceRecord> existing = attendanceDao.find(employee.getEmployeeId(), form.workDate());
        long id;
        if (existing.isPresent()) {
            id = existing.get().getAttendanceId();
            attendanceDao.update(id, in, out, status, late, source, Validator.clean(form.note()));
        } else {
            id = attendanceDao.insert(employee.getEmployeeId(), form.workDate(), in, out, status, late, source,
                    Validator.clean(form.note()));
        }
        if (!"API".equals(source)) {
            audit.record("ATTENDANCE_SAVED", "Attendance", id, employee.getEmployeeCode() + " " + form.workDate() + " " + status);
        }
        return attendanceDao.findById(id).orElseThrow();
    }

    public void delete(long attendanceId) {
        AttendanceRecord r = attendanceDao.findById(attendanceId)
                .orElseThrow(() -> NotFoundException.of("bản ghi chấm công", attendanceId));
        attendanceDao.delete(attendanceId);
        audit.record("ATTENDANCE_DELETED", "Attendance", attendanceId, r.getEmployeeCode() + " " + r.getWorkDate());
    }

    public long countCheckedInToday() {
        return attendanceDao.countCheckedIn(LocalDate.now(clock));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    private static long requireEmployee(AuthUser user) {
        if (user.getEmployeeId() == null) {
            throw new ForbiddenException("Tài khoản của bạn chưa liên kết hồ sơ nhân viên.");
        }
        return user.getEmployeeId();
    }
}
