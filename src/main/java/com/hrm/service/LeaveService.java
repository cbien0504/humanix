package com.hrm.service;

import com.hrm.dao.EmployeeDao;
import com.hrm.dao.HolidayDao;
import com.hrm.dao.LeaveDao;
import com.hrm.exception.BusinessException;
import com.hrm.exception.ForbiddenException;
import com.hrm.exception.NotFoundException;
import com.hrm.model.LeaveBalance;
import com.hrm.model.LeaveFilter;
import com.hrm.model.LeaveRequest;
import com.hrm.model.LeaveRequestForm;
import com.hrm.model.LeaveStatus;
import com.hrm.model.LeaveType;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.security.AuthUser;
import com.hrm.security.Role;
import com.hrm.util.Validator;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Leave requests and approval workflow (UC-23 … UC-27, BR-20 … BR-25).
 * <pre>
 * PENDING ──approve──▶ APPROVED ──cancel (before start)──▶ CANCELLED
 *    │  └────reject───▶ REJECTED
 *    └──────cancel────▶ CANCELLED
 * </pre>
 * Balances are kept in sync by the database trigger; this service validates first so the user
 * gets a clear message, and status changes use an optimistic "WHERE Status = expected" update.
 */
public class LeaveService {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LeaveDao leaveDao;
    private final EmployeeDao employeeDao;
    private final HolidayDao holidayDao;
    private final AuditService audit;
    private final Clock clock;
    private final int maxPastDays;

    public LeaveService(LeaveDao leaveDao, EmployeeDao employeeDao, HolidayDao holidayDao, AuditService audit,
                        Clock clock, int maxPastDays) {
        this.leaveDao = leaveDao;
        this.employeeDao = employeeDao;
        this.holidayDao = holidayDao;
        this.audit = audit;
        this.clock = clock;
        this.maxPastDays = maxPastDays;
    }

    public List<LeaveType> leaveTypes() {
        return leaveDao.leaveTypes();
    }

    public List<LeaveBalance> balances(AuthUser user, int year) {
        return leaveDao.balances(requireEmployee(user), year);
    }

    /** Number of working days in a range (preview while the user fills the form). */
    public int workingDays(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0;
        }
        return calculator(start, end).count(start, end);
    }

    public Page<LeaveRequest> myRequests(AuthUser user, LeaveFilter filter, PageRequest page) {
        return leaveDao.search(filter.scopedTo(requireEmployee(user), null), page);
    }

    /** Requests visible to an approver: HR all, managers their team. */
    public Page<LeaveRequest> approvalQueue(AuthUser user, LeaveFilter filter, PageRequest page) {
        if (user.isHrOrAdmin()) {
            return leaveDao.search(filter.scopedTo(null, null), page);
        }
        if (user.hasRole(Role.MANAGER) && user.getEmployeeId() != null) {
            return leaveDao.search(filter.scopedTo(null, user.getEmployeeId()), page);
        }
        throw new ForbiddenException();
    }

    public LeaveRequest submit(AuthUser user, LeaveRequestForm form) {
        long employeeId = requireEmployee(user);
        LocalDate today = LocalDate.now(clock);
        LeaveRequestForm f = new LeaveRequestForm(form.leaveTypeId(), form.startDate(), form.endDate(), Validator.clean(form.reason()));
        Validator v = new Validator()
                .required("leaveTypeId", f.leaveTypeId(), "Vui lòng chọn loại nghỉ phép.")
                .required("startDate", f.startDate(), "Vui lòng chọn ngày bắt đầu.")
                .required("endDate", f.endDate(), "Vui lòng chọn ngày kết thúc.")
                .required("reason", f.reason(), "Vui lòng nhập lý do.")
                .maxLength("reason", f.reason(), 500)
                .notBefore("endDate", f.endDate(), f.startDate(), "Ngày kết thúc phải từ ngày bắt đầu trở đi.");
        v.throwIfInvalid();
        v.check(f.startDate().getYear() == f.endDate().getYear(), "endDate",
                        "Đơn nghỉ không được kéo dài sang năm sau – vui lòng tách thành 2 đơn.")
                .check(!f.startDate().isBefore(today.minusDays(maxPastDays)), "startDate",
                        "Chỉ được tạo đơn cho ngày nghỉ trong vòng " + maxPastDays + " ngày trước.")
                .throwIfInvalid();

        LeaveType type = leaveDao.leaveType(f.leaveTypeId()).filter(LeaveType::active)
                .orElseThrow(() -> new NotFoundException("Loại nghỉ phép không tồn tại."));
        int days = calculator(f.startDate(), f.endDate()).count(f.startDate(), f.endDate());
        if (days == 0) {
            throw new BusinessException("Khoảng thời gian đã chọn không có ngày làm việc nào (cuối tuần/ngày lễ).");
        }
        BigDecimal totalDays = BigDecimal.valueOf(days);
        if (type.maxDaysPerRequest() != null && totalDays.compareTo(type.maxDaysPerRequest()) > 0) {
            throw new BusinessException(type.leaveTypeName() + " tối đa " + type.maxDaysPerRequest().stripTrailingZeros().toPlainString()
                    + " ngày mỗi đơn.");
        }
        List<LeaveRequest> overlaps = leaveDao.findOverlapping(employeeId, f.startDate(), f.endDate());
        if (!overlaps.isEmpty()) {
            throw new BusinessException("Trùng thời gian với đơn khác: " + overlaps.stream()
                    .map(o -> o.getStartDate().format(DMY) + "–" + o.getEndDate().format(DMY) + " (" + o.getStatus().getLabel() + ")")
                    .collect(Collectors.joining(", ")) + ".");
        }
        if (type.requiresBalance()) {
            LeaveBalance balance = leaveDao.balances(employeeId, f.startDate().getYear()).stream()
                    .filter(b -> b.leaveTypeId() == type.leaveTypeId()).findFirst()
                    .orElseThrow(() -> new BusinessException("Chưa có số dư " + type.leaveTypeName().toLowerCase(Locale.ROOT)
                            + " năm " + f.startDate().getYear() + ". Vui lòng liên hệ phòng Nhân sự."));
            if (balance.getAvailableDays().compareTo(totalDays) < 0) {
                throw new BusinessException("Không đủ số ngày phép: còn " + balance.getAvailableDays().stripTrailingZeros().toPlainString()
                        + " ngày khả dụng (đã trừ đơn đang chờ duyệt), đơn này cần " + days + " ngày.");
            }
        }
        long id = leaveDao.insert(employeeId, f, totalDays);
        audit.record("LEAVE_SUBMITTED", "LeaveRequest", id, type.leaveTypeCode() + " " + f.startDate() + "→" + f.endDate() + " (" + days + "d)");
        return leaveDao.findById(id).orElseThrow();
    }

    public LeaveRequest cancel(AuthUser user, long id) {
        LeaveRequest r = load(id);
        if (user.getEmployeeId() == null || r.getEmployeeId() != user.getEmployeeId()) {
            throw new ForbiddenException("Bạn chỉ có thể hủy đơn của chính mình.");
        }
        LocalDate today = LocalDate.now(clock);
        boolean cancellable = r.getStatus() == LeaveStatus.PENDING
                || (r.getStatus() == LeaveStatus.APPROVED && r.getStartDate().isAfter(today));
        if (!cancellable) {
            throw new BusinessException("Chỉ hủy được đơn đang chờ duyệt hoặc đơn đã duyệt chưa tới ngày nghỉ.");
        }
        update(r, r.getStatus(), LeaveStatus.CANCELLED, null, null);
        audit.record("LEAVE_CANCELLED", "LeaveRequest", id, null);
        return load(id);
    }

    public LeaveRequest approve(AuthUser approver, long id, String comment) {
        return decide(approver, id, LeaveStatus.APPROVED, Validator.clean(comment));
    }

    public LeaveRequest reject(AuthUser approver, long id, String comment) {
        String reason = Validator.clean(comment);
        new Validator().required("comment", reason, "Vui lòng nhập lý do từ chối.").maxLength("comment", reason, 500).throwIfInvalid();
        return decide(approver, id, LeaveStatus.REJECTED, reason);
    }

    public int initBalances(int year) {
        int rows = leaveDao.initBalances(year);
        audit.record("LEAVE_BALANCES_INITIALISED", "LeaveBalance", year, rows + " dòng");
        return rows;
    }

    public long countPending(AuthUser user) {
        if (user.isHrOrAdmin()) {
            return leaveDao.countPending(null);
        }
        return user.getEmployeeId() == null ? 0 : leaveDao.countPending(user.getEmployeeId());
    }

    // ------------------------------------------------------------------ internals

    private LeaveRequest decide(AuthUser approver, long id, LeaveStatus target, String comment) {
        LeaveRequest r = load(id);
        if (approver.getEmployeeId() == null) {
            throw new ForbiddenException("Tài khoản duyệt đơn phải liên kết hồ sơ nhân viên.");
        }
        if (r.getEmployeeId() == approver.getEmployeeId()) {
            throw new ForbiddenException("Bạn không thể tự duyệt đơn nghỉ phép của chính mình.");
        }
        boolean allowed = approver.isHrOrAdmin()
                || (approver.hasRole(Role.MANAGER) && employeeDao.isInTeam(approver.getEmployeeId(), r.getEmployeeId()));
        if (!allowed) {
            throw new ForbiddenException("Bạn không phải người quản lý của nhân viên này.");
        }
        if (r.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException("Đơn đã ở trạng thái \"" + r.getStatus().getLabel() + "\", không thể xử lý lại.");
        }
        update(r, LeaveStatus.PENDING, target, approver.getEmployeeId(), comment);
        audit.record(target == LeaveStatus.APPROVED ? "LEAVE_APPROVED" : "LEAVE_REJECTED", "LeaveRequest", id,
                r.getEmployeeCode() + " " + r.getStartDate() + "→" + r.getEndDate());
        return load(id);
    }

    private void update(LeaveRequest r, LeaveStatus expected, LeaveStatus target, Long approverId, String comment) {
        int updated = leaveDao.changeStatus(r.getLeaveRequestId(), expected, target, approverId, comment);
        if (updated == 0) {
            throw new BusinessException("Đơn vừa được người khác cập nhật. Vui lòng tải lại trang.");
        }
    }

    private LeaveRequest load(long id) {
        return leaveDao.findById(id).orElseThrow(() -> NotFoundException.of("đơn nghỉ phép", id));
    }

    private WorkingDayCalculator calculator(LocalDate from, LocalDate to) {
        return new WorkingDayCalculator(holidayDao.findBetween(from, to));
    }

    private static long requireEmployee(AuthUser user) {
        if (user.getEmployeeId() == null) {
            throw new ForbiddenException("Tài khoản của bạn chưa liên kết hồ sơ nhân viên.");
        }
        return user.getEmployeeId();
    }
}
