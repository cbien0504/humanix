package com.hrm.service;

import com.hrm.dao.DepartmentDao;
import com.hrm.dao.EmployeeDao;
import com.hrm.dao.PositionDao;
import com.hrm.exception.DataIntegrityException;
import com.hrm.exception.ValidationException;
import com.hrm.model.Employee;
import com.hrm.model.EmployeeForm;
import com.hrm.model.EmployeeStatus;
import com.hrm.model.ImportMode;
import com.hrm.model.ImportResult;
import com.hrm.model.ImportRowError;
import com.hrm.testsupport.DirectTransactions;
import com.hrm.testsupport.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeImportServiceTest {

    private static final String HEADER = "employee_code,full_name,gender,date_of_birth,email,phone,national_id,"
            + "department_code,position_code,manager_code,hire_date,status,base_salary,allowance,dependents";
    private static final String VINH = "EMP0150,Đoàn Quang Vinh,M,1991-02-14,vinh.dq@example.com,0916000150,001091000150,"
            + "IT,TL,EMP0011,2026-11-17,ACTIVE,\"30.000.000\",\"2,000,000\",1";
    private static final String THAO = "EMP0151,Mai Phương Thảo,Nữ,05/09/1998,thao.mp@example.com,0916000151,,"
            + "IT,DEV,EMP0150,17/11/2026,Thử việc,15500000,500000,0";

    @Mock
    private EmployeeDao employeeDao;
    @Mock
    private DepartmentDao departmentDao;
    @Mock
    private PositionDao positionDao;
    @Mock
    private AuditService audit;

    private final DirectTransactions tx = new DirectTransactions();
    private EmployeeImportService service;

    @BeforeEach
    void setUp() {
        EmployeeService employeeService = new EmployeeService(employeeDao, departmentDao, positionDao, audit, tx, Fixtures.clock());
        service = new EmployeeImportService(employeeService, employeeDao, departmentDao, positionDao, tx, audit);

        lenient().when(departmentDao.findAll()).thenReturn(List.of(Fixtures.department(4, "IT", true),
                Fixtures.department(9, "OLD", false)));
        lenient().when(positionDao.findAll()).thenReturn(List.of(Fixtures.position(6, "DEV", 12_000_000, 28_000_000),
                Fixtures.position(3, "TL", 25_000_000, 45_000_000)));
        lenient().when(departmentDao.findById(4)).thenReturn(Optional.of(Fixtures.department(4, "IT", true)));
        lenient().when(positionDao.findById(6)).thenReturn(Optional.of(Fixtures.position(6, "DEV", 12_000_000, 28_000_000)));
        lenient().when(positionDao.findById(3)).thenReturn(Optional.of(Fixtures.position(3, "TL", 25_000_000, 45_000_000)));
        lenient().when(employeeDao.findByCode(anyString())).thenReturn(Optional.empty());
        lenient().when(employeeDao.existsByEmail(anyString())).thenReturn(false);
        Employee manager = Fixtures.employeeRecord(11, "EMP0011");
        lenient().when(employeeDao.findByCode("EMP0011")).thenReturn(Optional.of(manager));
        lenient().when(employeeDao.findById(11)).thenReturn(Optional.of(manager));
    }

    private ImportResult importCsv(ImportMode mode, String... rows) throws IOException {
        String csv = HEADER + "\n" + String.join("\n", rows) + "\n";
        return service.importFile(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "nhan-vien.csv", mode);
    }

    private void databaseAcceptsInserts() {
        Employee vinh = Fixtures.employeeRecord(150, "EMP0150");
        when(employeeDao.insert(any())).thenReturn(150L, 151L);
        when(employeeDao.findById(150)).thenReturn(Optional.of(vinh));
        lenient().when(employeeDao.findById(151)).thenReturn(Optional.of(Fixtures.employeeRecord(151, "EMP0151")));
        when(employeeDao.findByCode("EMP0150")).thenReturn(Optional.empty(), Optional.of(vinh));
    }

    @Nested
    class Modes {

        @Test
        void validateOnlyWritesNothing() throws IOException {
            ImportResult r = importCsv(ImportMode.VALIDATE_ONLY, VINH, THAO);

            assertThat(r.totalRows()).isEqualTo(2);
            assertThat(r.validRows()).isEqualTo(2);
            assertThat(r.importedRows()).isZero();
            assertThat(r.errors()).isEmpty();
            verify(employeeDao, never()).insert(any());
            verify(audit).record(eq("DATA_IMPORTED"), eq("Employee"), isNull(),
                    eq("nhan-vien.csv [VALIDATE_ONLY] 2 dòng, 0 đã import, 0 dòng lỗi"));
        }

        @Test
        void allOrNothingImportsEveryRowInOneTransactionAndResolvesManagersFromTheFile() throws IOException {
            databaseAcceptsInserts();

            ImportResult r = importCsv(ImportMode.ALL_OR_NOTHING, VINH, THAO);

            assertThat(r.importedRows()).isEqualTo(2);
            assertThat(tx.started()).isEqualTo(1);
            assertThat(tx.committed()).isEqualTo(1);
            ArgumentCaptor<EmployeeForm> inserted = ArgumentCaptor.forClass(EmployeeForm.class);
            verify(employeeDao, times(2)).insert(inserted.capture());
            EmployeeForm vinh = inserted.getAllValues().get(0);
            EmployeeForm thao = inserted.getAllValues().get(1);
            assertThat(vinh.baseSalary()).isEqualByComparingTo("30000000");
            assertThat(vinh.allowance()).isEqualByComparingTo("2000000");
            assertThat(vinh.managerId()).isEqualTo(11L);
            assertThat(vinh.status()).isEqualTo(EmployeeStatus.ACTIVE);
            assertThat(thao.gender()).isEqualTo("F");
            assertThat(thao.dateOfBirth()).isEqualTo(LocalDate.of(1998, 9, 5));
            assertThat(thao.hireDate()).isEqualTo(LocalDate.of(2026, 11, 17));
            assertThat(thao.status()).isEqualTo(EmployeeStatus.PROBATION);
            assertThat(thao.managerId()).as("manager created by the previous row").isEqualTo(150L);
        }

        @Test
        void allOrNothingWritesNothingWhenOneRowIsInvalid() throws IOException {
            ImportResult r = importCsv(ImportMode.ALL_OR_NOTHING, VINH, THAO.replace("15500000", "-1"));

            assertThat(r.importedRows()).isZero();
            assertThat(r.errors()).extracting(ImportRowError::row, ImportRowError::column, ImportRowError::message)
                    .containsExactly(tuple(3, "base_salary", "Lương cơ bản không được âm."));
            assertThat(tx.started()).isZero();
            verify(employeeDao, never()).insert(any());
        }

        @Test
        void allOrNothingRollsBackWhenTheDatabaseRejectsALaterRow() throws IOException {
            when(employeeDao.insert(any())).thenReturn(150L)
                    .thenThrow(new DataIntegrityException("Email đã được dùng cho một nhân viên khác.", "email",
                            "UQ_Employees_Email", null));
            when(employeeDao.findById(150)).thenReturn(Optional.of(Fixtures.employeeRecord(150, "EMP0150")));
            when(employeeDao.findByCode("EMP0150")).thenReturn(Optional.empty(),
                    Optional.of(Fixtures.employeeRecord(150, "EMP0150")));

            ImportResult r = importCsv(ImportMode.ALL_OR_NOTHING, VINH, THAO);

            assertThat(r.importedRows()).isZero();
            assertThat(tx.rolledBack()).isEqualTo(1);
            assertThat(r.errors()).extracting(ImportRowError::row, ImportRowError::column, ImportRowError::message)
                    .containsExactly(tuple(3, "email", "Email đã được dùng cho một nhân viên khác."));
        }

        @Test
        void validOnlyImportsTheGoodRowsEachInItsOwnTransaction() throws IOException {
            when(employeeDao.insert(any())).thenReturn(150L);
            when(employeeDao.findById(150)).thenReturn(Optional.of(Fixtures.employeeRecord(150, "EMP0150")));

            ImportResult r = importCsv(ImportMode.VALID_ONLY, VINH, THAO.replace("thao.mp@example.com", "abc@"));

            assertThat(r.importedRows()).isEqualTo(1);
            assertThat(r.invalidRows()).isEqualTo(1);
            assertThat(tx.committed()).isEqualTo(1);
            assertThat(r.errors()).extracting(ImportRowError::row, ImportRowError::column, ImportRowError::value)
                    .containsExactly(tuple(3, "email", "abc@"));
            verify(audit).record(eq("DATA_IMPORTED"), eq("Employee"), isNull(), contains("1 đã import, 1 dòng lỗi"));
        }

        @Test
        void aRowWhoseManagerRowFailedIsReportedInsteadOfLinkedToNobody() throws IOException {
            ImportResult r = importCsv(ImportMode.VALID_ONLY, VINH.replace("vinh.dq@example.com", "sai-email"), THAO);

            assertThat(r.importedRows()).isZero();
            assertThat(r.errors()).extracting(ImportRowError::row, ImportRowError::column)
                    .containsExactly(tuple(2, "email"), tuple(3, "manager_code"));
            assertThat(r.errors().get(1).message()).contains("Quản lý EMP0150 chưa được import");
        }
    }

    @Nested
    class RowChecks {

        private List<ImportRowError> errorsOf(String... rows) throws IOException {
            return importCsv(ImportMode.VALIDATE_ONLY, rows).errors();
        }

        @Test
        void duplicatesInsideTheFileAndAgainstTheDatabase() throws IOException {
            when(employeeDao.findByCode("EMP0001")).thenReturn(Optional.of(Fixtures.employeeRecord(1, "EMP0001")));
            when(employeeDao.existsByEmail("used@example.com")).thenReturn(true);
            String row = "%s,Nguyễn Văn A,M,1990-01-01,%s,,%s,IT,DEV,,2026-11-17,,15000000,0,0";

            List<ImportRowError> errors = errorsOf(
                    row.formatted("EMP0160", "a@example.com", "001090000160"),
                    row.formatted("EMP0160", "b@example.com", ""),
                    row.formatted("EMP0161", "a@example.com", ""),
                    row.formatted("EMP0162", "c@example.com", "001090000160"),
                    row.formatted("EMP0001", "d@example.com", ""),
                    row.formatted("EMP0163", "used@example.com", ""));

            assertThat(errors).extracting(ImportRowError::row, ImportRowError::column, ImportRowError::message).containsExactly(
                    tuple(3, "employee_code", "Mã nhân viên bị trùng trong file."),
                    tuple(4, "email", "Email bị trùng trong file."),
                    tuple(5, "national_id", "Số CCCD bị trùng trong file."),
                    tuple(6, "employee_code", "Mã nhân viên đã tồn tại trong hệ thống."),
                    tuple(7, "email", "Email đã được dùng trong hệ thống."));
        }

        @Test
        void referencesMustExistAndBeActive() throws IOException {
            String row = "%s,Nguyễn Văn A,M,1990-01-01,%s@example.com,,,%s,%s,%s,2026-11-17,,15000000,0,0";

            List<ImportRowError> errors = errorsOf(
                    row.formatted("EMP0170", "a", "XYZ", "DEV", ""),
                    row.formatted("EMP0171", "b", "OLD", "DEV", ""),
                    row.formatted("EMP0172", "c", "IT", "ABC", ""),
                    row.formatted("EMP0173", "d", "IT", "DEV", "EMP9999"),
                    row.formatted("EMP0174", "e", "IT", "DEV", "EMP0174"));

            assertThat(errors).extracting(ImportRowError::row, ImportRowError::column).containsExactly(
                    tuple(2, "department_code"), tuple(3, "department_code"), tuple(4, "position_code"),
                    tuple(5, "manager_code"), tuple(6, "manager_code"));
            assertThat(errors.get(4).message()).isEqualTo("Nhân viên không thể tự quản lý chính mình.");
        }

        @Test
        void formatsAreCheckedStrictly() throws IOException {
            List<ImportRowError> errors = errorsOf(
                    "EMP0180,,X,31/02/1990,a@example.com,913000112,,IT,DEV,,2026-11-17,Bogus,15.00.000,0,hai");

            assertThat(errors).extracting(ImportRowError::column).containsExactly(
                    "full_name", "gender", "date_of_birth", "status", "base_salary", "dependents");
            assertThat(errors.get(0).message()).isEqualTo("Bắt buộc nhập.");
        }

        @Test
        void serviceRulesAreMappedBackToColumnsWithAHintForLostLeadingZeros() throws IOException {
            List<ImportRowError> errors = errorsOf(
                    "EMP0181,Nguyễn Văn B,M,1990-01-01,b@example.com,913000112,00109000018,IT,DEV,,2026-11-17,,90000000,0,0");

            assertThat(errors).extracting(ImportRowError::column)
                    .containsExactlyInAnyOrder("phone", "national_id", "base_salary");
            assertThat(errors).filteredOn(e -> e.column().equals("phone")).singleElement()
                    .satisfies(e -> assertThat(e.message()).contains("Excel đã bỏ số 0 ở đầu"));
            assertThat(errors).filteredOn(e -> e.column().equals("base_salary")).singleElement()
                    .satisfies(e -> assertThat(e.message()).contains("12.000.000 – 28.000.000"));
        }

        @Test
        void terminatedEmployeesAreNotImported() throws IOException {
            List<ImportRowError> errors = errorsOf(
                    "EMP0182,Nguyễn Văn C,M,1990-01-01,c@example.com,,,IT,DEV,,2026-11-17,Đã nghỉ việc,15000000,0,0");

            assertThat(errors).extracting(ImportRowError::message).containsExactly("Không import nhân viên đã nghỉ việc.");
        }
    }

    @Nested
    class Files {

        @Test
        void missingRequiredColumnsAreReportedForTheWholeFile() {
            byte[] csv = "code,name\nA,B\n".getBytes(StandardCharsets.UTF_8);

            assertThatThrownBy(() -> service.importFile(new ByteArrayInputStream(csv), "a.csv", ImportMode.VALIDATE_ONLY))
                    .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getFieldErrors().get("file"))
                            .contains("employee_code", "base_salary"));
        }

        @Test
        void aHeaderWithoutDataIsRejected() {
            byte[] csv = (HEADER + "\n").getBytes(StandardCharsets.UTF_8);

            assertThatThrownBy(() -> service.importFile(new ByteArrayInputStream(csv), "a.csv", ImportMode.VALIDATE_ONLY))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("File không có dòng dữ liệu nào.");
        }

        @Test
        void templateListsEveryColumnInOrder() {
            assertThat(EmployeeImportService.template().lines().findFirst())
                    .contains(String.join(",", EmployeeImportService.COLUMNS));
        }
    }

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource(delimiter = '|', value = {
            "12000000|12000000",
            "12.000.000|12000000",
            "12,000,000|12000000",
            "12.000.000 ₫|12000000",
            "15.000.000 VND|15000000",
            "1,500,000.50|1500000.50",
            "1.500.000,5|1500000.5",
            "12000000.00|12000000.00",
            "-5000000|-5000000"
    })
    void moneyFormatsFoundInRealExports(String raw, BigDecimal expected) {
        assertThat(EmployeeImportService.toAmount(raw)).isEqualByComparingTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12.00.000", "1e6", "abc", "₫", "12.0000"})
    void ambiguousAmountsAreRejectedNotGuessed(String raw) {
        assertThatThrownBy(() -> EmployeeImportService.toAmount(raw)).isInstanceOf(NumberFormatException.class);
    }

    @Test
    void statusAndGenderAcceptVietnameseLabelsWithOrWithoutAccents() {
        List<ImportRowError> errors = new ArrayList<>();

        assertThat(EmployeeImportService.parseStatus("Chính thức", 2, errors)).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(EmployeeImportService.parseStatus("nghi dai han", 2, errors)).isEqualTo(EmployeeStatus.ON_LEAVE);
        assertThat(EmployeeImportService.parseStatus(null, 2, errors)).isEqualTo(EmployeeStatus.PROBATION);
        assertThat(EmployeeImportService.parseGender("nữ", 2, errors)).isEqualTo("F");
        assertThat(EmployeeImportService.parseGender("Khac", 2, errors)).isEqualTo("O");
        assertThat(errors).isEmpty();
    }
}
