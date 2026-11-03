package com.hrm.service;

import com.hrm.dao.DepartmentDao;
import com.hrm.dao.EmployeeDao;
import com.hrm.dao.PositionDao;
import com.hrm.dao.TransactionManager;
import com.hrm.exception.AppException;
import com.hrm.exception.DataIntegrityException;
import com.hrm.exception.ValidationException;
import com.hrm.model.Department;
import com.hrm.model.EmployeeForm;
import com.hrm.model.EmployeeStatus;
import com.hrm.model.ImportMode;
import com.hrm.model.ImportResult;
import com.hrm.model.ImportRowError;
import com.hrm.model.Position;
import com.hrm.service.TabularFileReader.DataRow;
import com.hrm.util.Validator;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EmployeeImportService {

    public static final List<String> COLUMNS = List.of("employee_code", "full_name", "gender", "date_of_birth", "email",
            "phone", "national_id", "tax_code", "address", "department_code", "position_code", "manager_code", "hire_date",
            "status", "base_salary", "allowance", "dependents", "bank_name", "bank_account");
    static final List<String> REQUIRED = List.of("employee_code", "full_name", "gender", "email", "department_code",
            "position_code", "hire_date", "base_salary");

    private static final Map<String, String> FIELD_TO_COLUMN = Map.ofEntries(
            Map.entry("employeeCode", "employee_code"), Map.entry("fullName", "full_name"), Map.entry("gender", "gender"),
            Map.entry("dateOfBirth", "date_of_birth"), Map.entry("email", "email"), Map.entry("phone", "phone"),
            Map.entry("nationalId", "national_id"), Map.entry("taxCode", "tax_code"), Map.entry("address", "address"),
            Map.entry("departmentId", "department_code"), Map.entry("positionId", "position_code"),
            Map.entry("managerId", "manager_code"), Map.entry("hireDate", "hire_date"), Map.entry("terminationDate", "status"),
            Map.entry("status", "status"), Map.entry("baseSalary", "base_salary"), Map.entry("allowance", "allowance"),
            Map.entry("dependents", "dependents"), Map.entry("bankAccount", "bank_account"), Map.entry("bankName", "bank_name"));

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final EmployeeService employeeService;
    private final EmployeeDao employeeDao;
    private final DepartmentDao departmentDao;
    private final PositionDao positionDao;
    private final TransactionManager tx;
    private final AuditService audit;

    public EmployeeImportService(EmployeeService employeeService, EmployeeDao employeeDao, DepartmentDao departmentDao,
                                 PositionDao positionDao, TransactionManager tx, AuditService audit) {
        this.employeeService = employeeService;
        this.employeeDao = employeeDao;
        this.departmentDao = departmentDao;
        this.positionDao = positionDao;
        this.tx = tx;
        this.audit = audit;
    }

    private record Pending(int row, EmployeeForm form, String managerCodeInFile) {
    }

    public ImportResult importFile(InputStream input, String fileName, ImportMode mode) throws IOException {
        List<DataRow> rows = TabularFileReader.read(input, fileName);
        if (rows.isEmpty()) {
            throw new ValidationException("file", "File không có dòng dữ liệu nào.");
        }
        List<String> missing = REQUIRED.stream().filter(c -> !rows.get(0).values().containsKey(c)).toList();
        if (!missing.isEmpty()) {
            throw new ValidationException("file", "Thiếu cột bắt buộc: " + String.join(", ", missing)
                    + ". Hãy dùng file mẫu.");
        }

        Map<String, Department> departments = departmentDao.findAll().stream()
                .collect(Collectors.toMap(d -> d.getDepartmentCode().toUpperCase(Locale.ROOT), d -> d));
        Map<String, Position> positions = positionDao.findAll().stream()
                .collect(Collectors.toMap(p -> p.getPositionCode().toUpperCase(Locale.ROOT), p -> p));

        List<ImportRowError> errors = new ArrayList<>();
        List<Pending> valid = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        Set<String> emails = new HashSet<>();
        Set<String> nationalIds = new HashSet<>();

        for (DataRow row : rows) {
            Pending pending = checkRow(row, departments, positions, codes, emails, nationalIds, errors);
            if (pending != null) {
                valid.add(pending);
            }
            String code = Validator.upper(row.get("employee_code"));
            if (code != null) {
                codes.add(code);
            }
        }

        int imported = switch (mode) {
            case VALIDATE_ONLY -> 0;
            case VALID_ONLY -> insertEach(valid, errors);
            case ALL_OR_NOTHING -> errors.isEmpty() ? insertAll(valid, errors) : 0;
        };
        errors.sort((a, b) -> Integer.compare(a.row(), b.row()));
        ImportResult result = new ImportResult(fileName, mode, rows.size(), valid.size(), imported, List.copyOf(errors));
        audit.record("DATA_IMPORTED", "Employee", null, fileName + " [" + mode + "] " + rows.size() + " dòng, "
                + imported + " đã import, " + result.invalidRows() + " dòng lỗi");
        return result;
    }

    public static String template() {
        return String.join(",", COLUMNS) + "\n"
                + "EMP0101,Nguyễn Văn Mẫu,M,1995-05-20,mau.nv@example.com,0912345678,001095012345,8012345678,"
                + "\"12 Láng Hạ, Hà Nội\",IT,DEV,EMP0013,2026-11-15,PROBATION,15000000,1000000,0,Vietcombank,0011000999999\n";
    }

    private Pending checkRow(DataRow row, Map<String, Department> departments, Map<String, Position> positions,
                             Set<String> codesSoFar, Set<String> emails, Set<String> nationalIds, List<ImportRowError> errors) {
        int n = row.rowNumber();
        int before = errors.size();
        for (String col : REQUIRED) {
            if (row.get(col) == null) {
                errors.add(new ImportRowError(n, col, "", "Bắt buộc nhập."));
            }
        }

        String code = Validator.upper(row.get("employee_code"));
        String email = Validator.lower(row.get("email"));
        String nationalId = Validator.clean(row.get("national_id"));
        if (code != null) {
            if (codesSoFar.contains(code)) {
                errors.add(new ImportRowError(n, "employee_code", code, "Mã nhân viên bị trùng trong file."));
            } else if (employeeDao.findByCode(code).isPresent()) {
                errors.add(new ImportRowError(n, "employee_code", code, "Mã nhân viên đã tồn tại trong hệ thống."));
            }
        }
        if (email != null) {
            if (!emails.add(email)) {
                errors.add(new ImportRowError(n, "email", email, "Email bị trùng trong file."));
            } else if (employeeDao.existsByEmail(email)) {
                errors.add(new ImportRowError(n, "email", email, "Email đã được dùng trong hệ thống."));
            }
        }
        if (nationalId != null && !nationalIds.add(nationalId)) {
            errors.add(new ImportRowError(n, "national_id", nationalId, "Số CCCD bị trùng trong file."));
        }

        String gender = parseGender(row.get("gender"), n, errors);
        LocalDate dob = parseDate(row, "date_of_birth", errors);
        LocalDate hireDate = parseDate(row, "hire_date", errors);
        EmployeeStatus status = parseStatus(row.get("status"), n, errors);
        BigDecimal baseSalary = parseMoney(row, "base_salary", errors);
        BigDecimal allowance = parseMoney(row, "allowance", errors);
        Integer dependents = parseInt(row, "dependents", errors);

        Long departmentId = null;
        String deptCode = Validator.upper(row.get("department_code"));
        if (deptCode != null) {
            Department d = departments.get(deptCode);
            if (d == null || !d.isActive()) {
                errors.add(new ImportRowError(n, "department_code", deptCode, "Mã phòng ban không tồn tại hoặc đã ngừng hoạt động."));
            } else {
                departmentId = d.getDepartmentId();
            }
        }
        Long positionId = null;
        String posCode = Validator.upper(row.get("position_code"));
        if (posCode != null) {
            Position p = positions.get(posCode);
            if (p == null || !p.isActive()) {
                errors.add(new ImportRowError(n, "position_code", posCode, "Mã chức danh không tồn tại."));
            } else {
                positionId = p.getPositionId();
            }
        }
        Long managerId = null;
        String managerInFile = null;
        String managerCode = Validator.upper(row.get("manager_code"));
        if (managerCode != null) {
            if (managerCode.equals(code)) {
                errors.add(new ImportRowError(n, "manager_code", managerCode, "Nhân viên không thể tự quản lý chính mình."));
            } else if (codesSoFar.contains(managerCode)) {
                managerInFile = managerCode;
            } else {
                var manager = employeeDao.findByCode(managerCode);
                if (manager.isEmpty()) {
                    errors.add(new ImportRowError(n, "manager_code", managerCode,
                            "Không tìm thấy quản lý (phải có trong hệ thống hoặc ở dòng phía trên)."));
                } else {
                    managerId = manager.get().getEmployeeId();
                }
            }
        }
        if (errors.size() > before) {
            return null;
        }

        EmployeeForm form = employeeService.normalize(new EmployeeForm(code, row.get("full_name"), gender, dob, email,
                row.get("phone"), row.get("address"), nationalId, row.get("tax_code"), row.get("bank_account"),
                row.get("bank_name"), departmentId, positionId, managerId, hireDate, null, status, baseSalary, allowance,
                dependents));
        try {
            employeeService.validate(null, form);
        } catch (ValidationException e) {
            e.getFieldErrors().forEach((field, message) -> {
                String column = FIELD_TO_COLUMN.getOrDefault(field, field);
                String value = nullToEmpty(row.get(column));
                errors.add(new ImportRowError(n, column, value, lostLeadingZero(column, value)
                        ? message + " Có thể Excel đã bỏ số 0 ở đầu – hãy định dạng cột này là Text." : message));
            });
            return null;
        }
        return new Pending(n, form, managerInFile);
    }

    private int insertEach(List<Pending> rows, List<ImportRowError> errors) {
        int count = 0;
        for (Pending p : rows) {
            try {
                tx.inTransaction(() -> employeeService.create(withManager(p)));
                count++;
            } catch (AppException e) {
                errors.add(toError(p, e));
            }
        }
        return count;
    }

    private int insertAll(List<Pending> rows, List<ImportRowError> errors) {
        Pending[] current = new Pending[1];
        try {
            tx.inTransaction(() -> {
                for (Pending p : rows) {
                    current[0] = p;
                    employeeService.create(withManager(p));
                }
            });
            return rows.size();
        } catch (AppException e) {
            errors.add(toError(current[0], e));
            return 0;
        }
    }

    private EmployeeForm withManager(Pending p) {
        if (p.managerCodeInFile() == null) {
            return p.form();
        }
        long managerId = employeeDao.findByCode(p.managerCodeInFile())
                .orElseThrow(() -> new ValidationException("managerId", "Quản lý " + p.managerCodeInFile()
                        + " chưa được import (dòng của quản lý bị lỗi)."))
                .getEmployeeId();
        EmployeeForm f = p.form();
        return new EmployeeForm(f.employeeCode(), f.fullName(), f.gender(), f.dateOfBirth(), f.email(), f.phone(), f.address(),
                f.nationalId(), f.taxCode(), f.bankAccount(), f.bankName(), f.departmentId(), f.positionId(), managerId,
                f.hireDate(), f.terminationDate(), f.status(), f.baseSalary(), f.allowance(), f.dependents());
    }

    private static ImportRowError toError(Pending p, AppException e) {
        String column = "";
        String message = e.getMessage();
        if (e instanceof DataIntegrityException di && di.getField() != null) {
            column = FIELD_TO_COLUMN.getOrDefault(di.getField(), di.getField());
        } else if (e instanceof ValidationException ve && !ve.getFieldErrors().isEmpty()) {
            var first = ve.getFieldErrors().entrySet().iterator().next();
            column = FIELD_TO_COLUMN.getOrDefault(first.getKey(), "");
            message = first.getValue();
        }
        return new ImportRowError(p == null ? 0 : p.row(), column, "", message);
    }

    static LocalDate parseDate(DataRow row, String column, List<ImportRowError> errors) {
        String v = row.get(column);
        if (v == null) {
            return null;
        }
        try {
            return v.contains("/") ? LocalDate.parse(v, DMY) : LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            errors.add(new ImportRowError(row.rowNumber(), column, v, "Ngày không hợp lệ (yyyy-MM-dd hoặc dd/MM/yyyy)."));
            return null;
        }
    }

    static BigDecimal parseMoney(DataRow row, String column, List<ImportRowError> errors) {
        String v = row.get(column);
        if (v == null) {
            return null;
        }
        try {
            return toAmount(v);
        } catch (NumberFormatException e) {
            errors.add(new ImportRowError(row.rowNumber(), column, v, "Số tiền không hợp lệ."));
            return null;
        }
    }

    static BigDecimal toAmount(String raw) {
        String s = raw.replaceAll("(?i)vnd|vnđ|[₫đ\\s\\u00A0]", "");
        String sign = s.startsWith("-") ? "-" : "";
        s = s.substring(sign.length());
        String fraction = "";
        int sep = Math.max(s.lastIndexOf('.'), s.lastIndexOf(','));
        if (sep >= 0 && s.length() - sep - 1 != 3) {
            fraction = s.substring(sep + 1);
            s = s.substring(0, sep);
        }
        if (!s.matches("\\d+|\\d{1,3}([.,]\\d{3})+") || !fraction.matches("\\d{0,2}")) {
            throw new NumberFormatException(raw);
        }
        return new BigDecimal(sign + s.replaceAll("[.,]", "") + (fraction.isEmpty() ? "" : "." + fraction));
    }

    static Integer parseInt(DataRow row, String column, List<ImportRowError> errors) {
        String v = row.get(column);
        if (v == null) {
            return null;
        }
        try {
            return Integer.valueOf(v);
        } catch (NumberFormatException e) {
            errors.add(new ImportRowError(row.rowNumber(), column, v, "Phải là số nguyên."));
            return null;
        }
    }

    static String parseGender(String value, int row, List<ImportRowError> errors) {
        if (value == null) {
            return null;
        }
        String v = stripAccents(value).toUpperCase(Locale.ROOT);
        return switch (v) {
            case "M", "NAM", "MALE" -> "M";
            case "F", "NU", "FEMALE" -> "F";
            case "O", "KHAC", "OTHER" -> "O";
            default -> {
                errors.add(new ImportRowError(row, "gender", value, "Giới tính phải là M/F/O hoặc Nam/Nữ/Khác."));
                yield null;
            }
        };
    }

    static EmployeeStatus parseStatus(String value, int row, List<ImportRowError> errors) {
        if (value == null) {
            return EmployeeStatus.PROBATION;
        }
        String v = stripAccents(value).toUpperCase(Locale.ROOT).replace(' ', '_');
        Map<String, EmployeeStatus> aliases = new HashMap<>();
        for (EmployeeStatus s : EmployeeStatus.values()) {
            aliases.put(s.name(), s);
            aliases.put(stripAccents(s.getLabel()).toUpperCase(Locale.ROOT).replace(' ', '_'), s);
        }
        EmployeeStatus status = aliases.get(v);
        if (status == null) {
            errors.add(new ImportRowError(row, "status", value, "Trạng thái không hợp lệ (PROBATION/ACTIVE/ON_LEAVE)."));
        } else if (status == EmployeeStatus.TERMINATED) {
            errors.add(new ImportRowError(row, "status", value, "Không import nhân viên đã nghỉ việc."));
            return null;
        }
        return status;
    }

    static String stripAccents(String s) {
        String n = Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.replace('đ', 'd').replace('Đ', 'D');
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    static boolean lostLeadingZero(String column, String value) {
        return ("phone".equals(column) && value.matches("[1-9]\\d{8}"))
                || ("national_id".equals(column) && value.matches("[1-9]\\d{10}"));
    }
}
