package com.hrm.service;

import com.hrm.dao.DepartmentDao;
import com.hrm.dao.EmployeeDao;
import com.hrm.dao.PositionDao;
import com.hrm.dao.TransactionManager;
import com.hrm.exception.BusinessException;
import com.hrm.exception.ForbiddenException;
import com.hrm.exception.NotFoundException;
import com.hrm.model.Department;
import com.hrm.model.Employee;
import com.hrm.model.EmployeeFilter;
import com.hrm.model.EmployeeForm;
import com.hrm.model.EmployeeOption;
import com.hrm.model.EmployeeStatus;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.model.Position;
import com.hrm.model.SalaryChange;
import com.hrm.security.AuthUser;
import com.hrm.security.Role;
import com.hrm.util.Validator;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class EmployeeService {

    static final Pattern TAX_CODE = Pattern.compile("^\\d{10}(\\d{3})?$");
    static final String CODE_PREFIX = "EMP";

    private final EmployeeDao employeeDao;
    private final DepartmentDao departmentDao;
    private final PositionDao positionDao;
    private final AuditService audit;
    private final TransactionManager tx;
    private final Clock clock;

    public EmployeeService(EmployeeDao employeeDao, DepartmentDao departmentDao, PositionDao positionDao,
                           AuditService audit, TransactionManager tx, Clock clock) {
        this.employeeDao = employeeDao;
        this.departmentDao = departmentDao;
        this.positionDao = positionDao;
        this.audit = audit;
        this.tx = tx;
        this.clock = clock;
    }

    public Page<Employee> search(AuthUser user, EmployeeFilter filter, PageRequest page) {
        if (user.isHrOrAdmin()) {
            return employeeDao.search(filter.withTeamOf(null), page);
        }
        if (user.hasRole(Role.MANAGER) && user.getEmployeeId() != null) {
            return employeeDao.search(filter.withTeamOf(user.getEmployeeId()), page)
                    .map(Employee::withoutConfidentialData);
        }
        throw new ForbiddenException();
    }

    public Employee get(AuthUser user, long id) {
        Employee e = employeeDao.findById(id).orElseThrow(() -> NotFoundException.of("nhân viên", id));
        if (user.isHrOrAdmin() || isSelf(user, id)) {
            return e;
        }
        if (user.hasRole(Role.MANAGER) && user.getEmployeeId() != null && employeeDao.isInTeam(user.getEmployeeId(), id)) {
            return e.withoutConfidentialData();
        }
        throw new ForbiddenException("Bạn không có quyền xem hồ sơ nhân viên này.");
    }

    public Employee getByCode(AuthUser user, String code) {
        Employee e = employeeDao.findByCode(Validator.upper(code))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên mã " + code + "."));
        return get(user, e.getEmployeeId());
    }

    public List<SalaryChange> salaryHistory(AuthUser user, long id) {
        if (!user.isHrOrAdmin() && !isSelf(user, id)) {
            throw new ForbiddenException("Chỉ bộ phận nhân sự xem được lịch sử lương.");
        }
        return employeeDao.salaryHistory(id);
    }

    public List<EmployeeOption> options() {
        return employeeDao.options();
    }

    public String nextCode() {
        return employeeDao.nextCode(CODE_PREFIX);
    }

    public Employee create(EmployeeForm form) {
        EmployeeForm f = normalize(form);
        validate(null, f);
        long id = employeeDao.insert(f);
        audit.record("EMPLOYEE_CREATED", "Employee", id, f.employeeCode() + " – " + f.fullName());
        return employeeDao.findById(id).orElseThrow();
    }

    public Employee update(long id, EmployeeForm form) {
        Employee current = employeeDao.findById(id).orElseThrow(() -> NotFoundException.of("nhân viên", id));
        EmployeeForm f = normalize(form);
        validate(id, f);
        employeeDao.update(id, f);
        StringBuilder details = new StringBuilder(f.employeeCode());
        if (current.getBaseSalary() != null && f.baseSalary().compareTo(current.getBaseSalary()) != 0) {
            details.append(", lương ").append(current.getBaseSalary().toPlainString()).append(" → ").append(f.baseSalary().toPlainString());
        }
        if (current.getStatus() != f.status()) {
            details.append(", trạng thái ").append(current.getStatus()).append(" → ").append(f.status());
        }
        audit.record("EMPLOYEE_UPDATED", "Employee", id, details.toString());
        return employeeDao.findById(id).orElseThrow();
    }

    public void delete(long id) {
        Employee e = employeeDao.findById(id).orElseThrow(() -> NotFoundException.of("nhân viên", id));
        List<String> deps = employeeDao.dependencies(id);
        if (!deps.isEmpty()) {
            throw new BusinessException("Không thể xóa " + e.getEmployeeCode() + " vì đã có dữ liệu "
                    + String.join(", ", deps) + ". Hãy chuyển trạng thái sang \"Đã nghỉ việc\".");
        }
        tx.inTransaction(() -> employeeDao.delete(id));
        audit.record("EMPLOYEE_DELETED", "Employee", id, e.getEmployeeCode() + " – " + e.getFullName());
    }

    EmployeeForm normalize(EmployeeForm f) {
        return new EmployeeForm(Validator.upper(f.employeeCode()), Validator.clean(f.fullName()), Validator.upper(f.gender()),
                f.dateOfBirth(), Validator.lower(f.email()), Validator.clean(f.phone()), Validator.clean(f.address()),
                Validator.clean(f.nationalId()), Validator.clean(f.taxCode()), Validator.clean(f.bankAccount()),
                Validator.clean(f.bankName()), f.departmentId(), f.positionId(), f.managerId(), f.hireDate(),
                f.terminationDate(), f.status() == null ? EmployeeStatus.PROBATION : f.status(), f.baseSalary(),
                f.allowance() == null ? BigDecimal.ZERO : f.allowance(), f.dependents() == null ? 0 : f.dependents());
    }

    void validate(Long id, EmployeeForm f) {
        LocalDate today = LocalDate.now(clock);
        Validator v = new Validator()
                .required("employeeCode", f.employeeCode(), "Vui lòng nhập mã nhân viên.")
                .pattern("employeeCode", f.employeeCode(), Validator.CODE, "Mã gồm 3–20 ký tự: chữ in hoa, chữ số, dấu gạch ngang.")
                .required("fullName", f.fullName(), "Vui lòng nhập họ tên.")
                .maxLength("fullName", f.fullName(), 150)
                .required("gender", f.gender(), "Vui lòng chọn giới tính.")
                .check(f.gender() == null || List.of("M", "F", "O").contains(f.gender()), "gender", "Giới tính không hợp lệ.")
                .required("email", f.email(), "Vui lòng nhập email.")
                .email("email", f.email())
                .maxLength("email", f.email(), 150)
                .phone("phone", f.phone())
                .pattern("nationalId", f.nationalId(), Validator.NATIONAL_ID, "CCCD gồm đúng 12 chữ số.")
                .pattern("taxCode", f.taxCode(), TAX_CODE, "Mã số thuế gồm 10 hoặc 13 chữ số.")
                .maxLength("address", f.address(), 300)
                .maxLength("bankAccount", f.bankAccount(), 30)
                .required("departmentId", f.departmentId(), "Vui lòng chọn phòng ban.")
                .required("positionId", f.positionId(), "Vui lòng chọn chức danh.")
                .required("hireDate", f.hireDate(), "Vui lòng nhập ngày vào làm.")
                .check(f.hireDate() == null || !f.hireDate().isAfter(today.plusDays(90)), "hireDate",
                        "Ngày vào làm không được quá 90 ngày tính từ hôm nay.")
                .check(f.dateOfBirth() == null || f.dateOfBirth().isBefore(today), "dateOfBirth", "Ngày sinh không hợp lệ.")
                .check(f.dateOfBirth() == null || f.hireDate() == null || !f.dateOfBirth().plusYears(15).isAfter(f.hireDate()),
                        "dateOfBirth", "Nhân viên phải đủ 15 tuổi tại ngày vào làm.")
                .notBefore("terminationDate", f.terminationDate(), f.hireDate(), "Ngày nghỉ việc phải sau ngày vào làm.")
                .check(f.status() != EmployeeStatus.TERMINATED || f.terminationDate() != null, "terminationDate",
                        "Nhân viên đã nghỉ việc phải có ngày nghỉ việc.")
                .check(f.status() == EmployeeStatus.TERMINATED || f.terminationDate() == null, "status",
                        "Có ngày nghỉ việc thì trạng thái phải là \"Đã nghỉ việc\".")
                .required("baseSalary", f.baseSalary(), "Vui lòng nhập lương cơ bản.")
                .nonNegative("baseSalary", f.baseSalary(), "Lương cơ bản không được âm.")
                .nonNegative("allowance", f.allowance(), "Phụ cấp không được âm.")
                .range("dependents", f.dependents(), 0, 20, "Số người phụ thuộc từ 0 đến 20.");

        if (f.departmentId() != null) {
            Optional<Department> d = departmentDao.findById(f.departmentId());
            v.check(d.isPresent() && d.get().isActive(), "departmentId", "Phòng ban không tồn tại hoặc đã ngừng hoạt động.");
        }
        if (f.positionId() != null && !v.hasError("baseSalary")) {
            Optional<Position> p = positionDao.findById(f.positionId());
            v.check(p.isPresent() && p.get().isActive(), "positionId", "Chức danh không tồn tại hoặc đã ngừng sử dụng.");
            p.ifPresent(pos -> v.check(f.baseSalary() == null || pos.accepts(f.baseSalary()), "baseSalary",
                    "Lương cơ bản phải trong khung " + money(pos.getMinSalary()) + " – " + money(pos.getMaxSalary())
                            + " của chức danh " + pos.getPositionName() + "."));
        }
        if (f.managerId() != null) {
            v.check(id == null || !f.managerId().equals(id), "managerId", "Nhân viên không thể là quản lý của chính mình.");
            Optional<Employee> m = employeeDao.findById(f.managerId());
            v.check(m.isPresent() && m.get().getStatus() != EmployeeStatus.TERMINATED, "managerId",
                    "Người quản lý không tồn tại hoặc đã nghỉ việc.");
            if (id != null && !v.hasError("managerId")) {
                v.check(!employeeDao.managerChain(f.managerId()).contains(id), "managerId",
                        "Không hợp lệ: tạo vòng lặp quản lý (người này đang là cấp trên của quản lý được chọn).");
            }
        }
        v.throwIfInvalid();
    }

    private static boolean isSelf(AuthUser user, long employeeId) {
        return user.getEmployeeId() != null && user.getEmployeeId() == employeeId;
    }

    private static String money(BigDecimal value) {
        return NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(value);
    }
}
