package com.hrm.service;

import com.hrm.dao.PayrollDao;
import com.hrm.exception.BusinessException;
import com.hrm.exception.ForbiddenException;
import com.hrm.exception.NotFoundException;
import com.hrm.exception.ValidationException;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.model.PayrollPeriod;
import com.hrm.model.PayrollSettings;
import com.hrm.model.PayrollStatus;
import com.hrm.model.Payslip;
import com.hrm.model.SettingDefinition;
import com.hrm.report.ReportColumn;
import com.hrm.report.TabularReport;
import com.hrm.security.AuthUser;
import com.hrm.util.Validator;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PayrollService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Set<String> RATE_KEYS = Set.of(PayrollSettings.SOCIAL_INSURANCE_RATE,
            PayrollSettings.HEALTH_INSURANCE_RATE, PayrollSettings.UNEMPLOYMENT_INSURANCE_RATE);

    private final PayrollDao payrollDao;
    private final PayrollCalculator calculator;
    private final AuditService audit;
    private final Clock clock;

    public PayrollService(PayrollDao payrollDao, PayrollCalculator calculator, AuditService audit, Clock clock) {
        this.payrollDao = payrollDao;
        this.calculator = calculator;
        this.audit = audit;
        this.clock = clock;
    }

    public record SimulationRequest(BigDecimal baseSalary, BigDecimal allowance, Integer dependents) {
    }

    public List<PayrollPeriod> periods(Integer year) {
        return payrollDao.periods(year);
    }

    public PayrollPeriod period(long periodId) {
        return payrollDao.period(periodId).orElseThrow(() -> NotFoundException.of("kỳ lương", periodId));
    }

    public PayrollPeriod calculate(AuthUser user, int year, int month) {
        if (month < 1 || month > 12 || year < 2000 || year > 2100) {
            throw new ValidationException("month", "Kỳ lương không hợp lệ.");
        }
        if (YearMonth.of(year, month).isAfter(YearMonth.now(clock))) {
            throw new BusinessException("Không thể tính lương cho tháng trong tương lai.");
        }
        PayrollPeriod period = payrollDao.calculate(year, month, user.getUsername());
        audit.record("PAYROLL_CALCULATED", "PayrollPeriod", period.getPeriodId(),
                period.getLabel() + ": " + period.getEmployeeCount() + " NV, net " + period.getTotalNet().toPlainString());
        return period;
    }

    public PayrollPeriod approve(AuthUser user, long periodId) {
        return transition(user, periodId, PayrollStatus.CALCULATED, PayrollStatus.APPROVED, "PAYROLL_APPROVED",
                "Chỉ duyệt được kỳ lương ở trạng thái \"Đã tính\".");
    }

    public PayrollPeriod markPaid(AuthUser user, long periodId) {
        return transition(user, periodId, PayrollStatus.APPROVED, PayrollStatus.PAID, "PAYROLL_PAID",
                "Chỉ đánh dấu chi trả cho kỳ lương đã được duyệt.");
    }

    public Page<Payslip> payslips(long periodId, String keyword, Long departmentId, PageRequest page) {
        period(periodId);
        return payrollDao.payslips(periodId, keyword, departmentId, page);
    }

    public List<Map<String, Object>> departmentTotals(long periodId) {
        return payrollDao.departmentTotals(periodId);
    }

    public Payslip payslip(AuthUser user, long payrollId) {
        Payslip p = payrollDao.payslip(payrollId).orElseThrow(() -> NotFoundException.of("phiếu lương", payrollId));
        if (user.isHrOrAdmin()) {
            return p;
        }
        boolean owner = user.getEmployeeId() != null && user.getEmployeeId() == p.getEmployeeId();
        if (!owner) {
            throw new ForbiddenException("Bạn không có quyền xem phiếu lương này.");
        }
        if (p.getPeriodStatus().isRecalculable()) {
            throw new ForbiddenException("Phiếu lương chưa được duyệt.");
        }
        return p;
    }

    public List<Payslip> myPayslips(AuthUser user) {
        if (user.getEmployeeId() == null) {
            throw new ForbiddenException("Tài khoản của bạn chưa liên kết hồ sơ nhân viên.");
        }
        return payrollDao.payslipsOfEmployee(user.getEmployeeId());
    }

    public PayrollCalculator.Result simulate(SimulationRequest req) {
        BigDecimal allowance = req.allowance() == null ? BigDecimal.ZERO : req.allowance();
        int dependents = req.dependents() == null ? 0 : req.dependents();
        new Validator()
                .required("baseSalary", req.baseSalary(), "Vui lòng nhập lương.")
                .check(req.baseSalary() == null || (req.baseSalary().signum() >= 0
                        && req.baseSalary().compareTo(BigDecimal.valueOf(10_000_000_000L)) < 0), "baseSalary", "Lương không hợp lệ.")
                .nonNegative("allowance", allowance, "Phụ cấp không được âm.")
                .range("dependents", dependents, 0, 20, "Số người phụ thuộc từ 0 đến 20.")
                .throwIfInvalid();
        return calculator.simulate(req.baseSalary(), allowance, dependents, payrollDao.settings());
    }

    public PayrollSettings settings() {
        return payrollDao.settings();
    }

    public List<SettingDefinition> settingDefinitions() {
        return payrollDao.settingDefinitions();
    }

    public List<SettingDefinition> updateSettings(Map<String, BigDecimal> changes) {
        Map<String, BigDecimal> current = payrollDao.settings().values();
        Validator v = new Validator();
        for (Map.Entry<String, BigDecimal> e : changes.entrySet()) {
            String key = e.getKey();
            BigDecimal value = e.getValue();
            v.check(current.containsKey(key), key, "Tham số không tồn tại.")
             .required(key, value, "Vui lòng nhập giá trị.")
             .check(value == null || value.signum() >= 0, key, "Giá trị không được âm.")
             .check(value == null || !RATE_KEYS.contains(key) || value.compareTo(new BigDecimal("0.5")) < 0, key,
                     "Tỷ lệ phải nhỏ hơn 0,5 (ví dụ 0.08 = 8%).");
        }
        v.throwIfInvalid();
        Map<String, BigDecimal> applied = new LinkedHashMap<>();
        changes.forEach((key, value) -> {
            if (current.get(key).compareTo(value) != 0) {
                payrollDao.updateSetting(key, value);
                applied.put(key, value);
            }
        });
        if (!applied.isEmpty()) {
            audit.record("PAYROLL_SETTINGS_UPDATED", "PayrollSettings", null, applied.toString());
        }
        return payrollDao.settingDefinitions();
    }

    public TabularReport payrollSheet(long periodId) {
        PayrollPeriod p = period(periodId);
        ZoneId zone = clock.getZone();
        TabularReport report = new TabularReport("BẢNG LƯƠNG THÁNG " + p.getLabel(),
                List.of("Trạng thái: " + p.getStatus().getLabel() + " · Ngày công chuẩn: " + p.getStandardWorkDays().stripTrailingZeros().toPlainString(),
                        "Tính lúc: " + stamp(p.getCalculatedAt(), zone) + " bởi " + nullToDash(p.getCalculatedBy())
                                + (p.getApprovedAt() != null ? " · Duyệt lúc: " + stamp(p.getApprovedAt(), zone) + " bởi " + p.getApprovedBy() : ""),
                        "Xuất lúc: " + stamp(Instant.now(clock), zone)),
                List.of(ReportColumn.integer("STT"), ReportColumn.text("Mã NV", 10), ReportColumn.text("Họ tên", 24),
                        ReportColumn.text("Phòng ban", 24), ReportColumn.text("Chức danh", 22),
                        ReportColumn.money("Lương cơ bản"), ReportColumn.money("Phụ cấp"), ReportColumn.decimal("Công chuẩn"),
                        ReportColumn.decimal("Công đi làm"), ReportColumn.decimal("Nghỉ có lương"), ReportColumn.decimal("Công hưởng lương"),
                        ReportColumn.money("Lương theo công"), ReportColumn.money("Phụ cấp thực nhận"), ReportColumn.money("Tổng thu nhập"),
                        ReportColumn.money("BHXH 8%"), ReportColumn.money("BHYT 1,5%"), ReportColumn.money("BHTN 1%"),
                        ReportColumn.integer("Số NPT"), ReportColumn.money("Thu nhập tính thuế"), ReportColumn.money("Thuế TNCN"),
                        ReportColumn.money("Thực lĩnh"), ReportColumn.text("Ngân hàng", 14), ReportColumn.text("Số tài khoản", 18)));
        int i = 1;
        for (Payslip s : payrollDao.allPayslips(periodId)) {
            report.addRow(i++, s.getEmployeeCode(), s.getFullName(), s.getDepartmentName(), s.getPositionName(),
                    s.getBaseSalary(), s.getAllowance(), s.getStandardDays(), s.getWorkedDays(), s.getPaidLeaveDays(), s.getPaidDays(),
                    s.getSalaryAmount(), s.getAllowanceAmount(), s.getGrossSalary(), s.getSocialInsurance(), s.getHealthInsurance(),
                    s.getUnemploymentInsurance(), s.getDependents(), s.getTaxableIncome(), s.getPersonalIncomeTax(), s.getNetSalary(),
                    s.getBankName(), s.getBankAccount());
        }
        audit.record("PAYROLL_EXPORTED", "PayrollPeriod", periodId, p.getLabel());
        return report;
    }

    private PayrollPeriod transition(AuthUser user, long periodId, PayrollStatus from, PayrollStatus to, String action,
                                     String message) {
        PayrollPeriod p = period(periodId);
        if (p.getStatus() != from || payrollDao.changeStatus(periodId, from, to, user.getUsername()) == 0) {
            throw new BusinessException(message);
        }
        audit.record(action, "PayrollPeriod", periodId, p.getLabel());
        return period(periodId);
    }

    private static String stamp(Instant instant, ZoneId zone) {
        return instant == null ? "—" : STAMP.format(instant.atZone(zone));
    }

    private static String nullToDash(String s) {
        return s == null ? "—" : s;
    }
}
