package com.hrm.service;

import com.hrm.dao.DepartmentDao;
import com.hrm.dao.EmployeeDao;
import com.hrm.dao.PositionDao;
import com.hrm.exception.BusinessException;
import com.hrm.exception.DataIntegrityException;
import com.hrm.exception.ForbiddenException;
import com.hrm.exception.NotFoundException;
import com.hrm.exception.ValidationException;
import com.hrm.model.Employee;
import com.hrm.model.EmployeeFilter;
import com.hrm.model.EmployeeForm;
import com.hrm.model.EmployeeStatus;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.testsupport.DirectTransactions;
import com.hrm.testsupport.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.hrm.testsupport.Fixtures.vnd;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Employee records: business rules BR-01 … BR-07 and the role-based data scope (UC-12 … UC-16). */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeDao employeeDao;
    @Mock
    private DepartmentDao departmentDao;
    @Mock
    private PositionDao positionDao;
    @Mock
    private AuditService audit;

    private final DirectTransactions tx = new DirectTransactions();
    private EmployeeService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeService(employeeDao, departmentDao, positionDao, audit, tx, Fixtures.clock());
        lenient().when(departmentDao.findById(4)).thenReturn(Optional.of(Fixtures.department(4, "IT", true)));
        lenient().when(positionDao.findById(6)).thenReturn(Optional.of(Fixtures.position(6, "DEV", 12_000_000, 28_000_000)));
    }

    private ValidationException invalid(EmployeeForm form) {
        try {
            service.create(form);
        } catch (ValidationException e) {
            verify(employeeDao, never()).insert(any());
            return e;
        }
        throw new AssertionError("expected a ValidationException");
    }

    @Nested
    class Create {

        @Test
        void storesANormalisedRecordAndAudits() {
            when(employeeDao.insert(any())).thenReturn(50L);
            when(employeeDao.findById(50)).thenReturn(Optional.of(Fixtures.employeeRecord(50, "EMP0100")));

            service.create(Fixtures.employeeForm().code(" emp0100 ").fullName("  Nguyễn Văn Kiểm ")
                    .email(" Kiem.NV@Example.com ").status(null).allowance(null).dependents(null).build());

            ArgumentCaptor<EmployeeForm> stored = ArgumentCaptor.forClass(EmployeeForm.class);
            verify(employeeDao).insert(stored.capture());
            assertThat(stored.getValue().employeeCode()).isEqualTo("EMP0100");
            assertThat(stored.getValue().fullName()).isEqualTo("Nguyễn Văn Kiểm");
            assertThat(stored.getValue().email()).isEqualTo("kiem.nv@example.com");
            assertThat(stored.getValue().status()).isEqualTo(EmployeeStatus.PROBATION);
            assertThat(stored.getValue().allowance()).isZero();
            assertThat(stored.getValue().dependents()).isZero();
            verify(audit).record("EMPLOYEE_CREATED", "Employee", 50L, "EMP0100 – Nguyễn Văn Kiểm");
        }

        @Test
        void negativeSalaryIsRejectedWithAClearMessage() {
            ValidationException e = invalid(Fixtures.employeeForm().baseSalary(vnd(-5_000_000)).build());

            assertThat(e.getFieldErrors()).containsEntry("baseSalary", "Lương cơ bản không được âm.");
            verify(positionDao, never()).findById(anyLong());     // no salary-band message on top of it
        }

        @Test
        void salaryMustFitTheBandOfThePosition() {
            ValidationException e = invalid(Fixtures.employeeForm().baseSalary(vnd(90_000_000)).build());

            assertThat(e.getFieldErrors().get("baseSalary")).contains("12.000.000 – 28.000.000");
        }

        @Test
        void duplicateCodeFromTheUniqueIndexReachesTheCallerWithItsField() {
            when(employeeDao.insert(any())).thenThrow(new DataIntegrityException("Mã nhân viên đã tồn tại.", "employeeCode",
                    "UQ_Employees_EmployeeCode", null));

            assertThatThrownBy(() -> service.create(Fixtures.employeeForm().code("EMP0001").build()))
                    .isInstanceOfSatisfying(DataIntegrityException.class, e -> {
                        assertThat(e.getField()).isEqualTo("employeeCode");
                        assertThat(e.getHttpStatus()).isEqualTo(409);
                    });
            verifyNoInteractions(audit);
        }

        @Test
        void reportsEveryInvalidFieldAtOnce() {
            ValidationException e = invalid(Fixtures.employeeForm().code("x").fullName(" ").gender("X").email("abc@")
                    .phone("12345").nationalId("123").taxCode("12").dependents(21).build());

            assertThat(e.getFieldErrors()).containsKeys("employeeCode", "fullName", "gender", "email", "phone",
                    "nationalId", "taxCode", "dependents");
        }

        @Test
        void employeeMustBeFifteenAtHireDate() {
            ValidationException e = invalid(Fixtures.employeeForm().dateOfBirth(LocalDate.of(2015, 1, 1)).build());

            assertThat(e.getFieldErrors()).containsEntry("dateOfBirth", "Nhân viên phải đủ 15 tuổi tại ngày vào làm.");
        }

        @Test
        void hireDateCannotBeMoreThanNinetyDaysAhead() {
            ValidationException e = invalid(Fixtures.employeeForm().hireDate(Fixtures.TODAY.plusDays(91)).build());

            assertThat(e.getFieldErrors()).containsKey("hireDate");
        }

        @Test
        void terminationDateAndStatusGoTogether() {
            assertThat(invalid(Fixtures.employeeForm().status(EmployeeStatus.TERMINATED).build()).getFieldErrors())
                    .containsKey("terminationDate");
            assertThat(invalid(Fixtures.employeeForm().terminationDate(Fixtures.TODAY).build()).getFieldErrors())
                    .containsKey("status");
        }

        @Test
        void inactiveDepartmentIsRefused() {
            when(departmentDao.findById(9)).thenReturn(Optional.of(Fixtures.department(9, "OLD", false)));

            assertThat(invalid(Fixtures.employeeForm().departmentId(9L).build()).getFieldErrors()).containsKey("departmentId");
        }

        @Test
        void terminatedManagerIsRefused() {
            Employee gone = Fixtures.employeeRecord(30, "EMP0030");
            gone.setStatus(EmployeeStatus.TERMINATED);
            when(employeeDao.findById(30)).thenReturn(Optional.of(gone));

            assertThat(invalid(Fixtures.employeeForm().managerId(30L).build()).getFieldErrors()).containsKey("managerId");
        }
    }

    @Nested
    class Update {

        @Test
        void salaryAndStatusChangesAreAudited() {
            Employee current = Fixtures.employeeRecord(16, "EMP0016");
            current.setBaseSalary(vnd(15_000_000));
            current.setStatus(EmployeeStatus.PROBATION);
            when(employeeDao.findById(16)).thenReturn(Optional.of(current));

            service.update(16, Fixtures.employeeForm().code("EMP0016").baseSalary(vnd(18_000_000))
                    .status(EmployeeStatus.ACTIVE).build());

            verify(employeeDao).update(eq(16L), any());
            verify(audit).record(eq("EMPLOYEE_UPDATED"), eq("Employee"), eq(16L),
                    contains("lương 15000000 → 18000000, trạng thái PROBATION → ACTIVE"));
        }

        @Test
        void anEmployeeCannotManageHimself() {
            when(employeeDao.findById(16)).thenReturn(Optional.of(Fixtures.employeeRecord(16, "EMP0016")));

            assertThatThrownBy(() -> service.update(16, Fixtures.employeeForm().managerId(16L).build()))
                    .isInstanceOfSatisfying(ValidationException.class,
                            e -> assertThat(e.getFieldErrors()).containsKey("managerId"));
        }

        @Test
        void managementCyclesAreDetected() {
            // 13 → manages 16; making 16 the manager of 13 would close a loop
            when(employeeDao.findById(13)).thenReturn(Optional.of(Fixtures.employeeRecord(13, "EMP0013")));
            when(employeeDao.findById(16)).thenReturn(Optional.of(Fixtures.employeeRecord(16, "EMP0016")));
            when(employeeDao.managerChain(16)).thenReturn(List.of(13L, 11L, 1L));

            assertThatThrownBy(() -> service.update(13, Fixtures.employeeForm().code("EMP0013").managerId(16L).build()))
                    .isInstanceOfSatisfying(ValidationException.class,
                            e -> assertThat(e.getFieldErrors().get("managerId")).contains("vòng lặp quản lý"));
            verify(employeeDao, never()).update(anyLong(), any());
        }

        @Test
        void unknownEmployeeIsNotFound() {
            when(employeeDao.findById(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(404, Fixtures.employeeForm().build())).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class Delete {

        @Test
        void employeesWithBusinessDataMustBeTerminatedInstead() {
            when(employeeDao.findById(16)).thenReturn(Optional.of(Fixtures.employeeRecord(16, "EMP0016")));
            when(employeeDao.dependencies(16)).thenReturn(List.of("chấm công", "bảng lương"));

            assertThatThrownBy(() -> service.delete(16))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("chấm công, bảng lương")
                    .hasMessageContaining("Đã nghỉ việc");
            verify(employeeDao, never()).delete(anyLong());
        }

        @Test
        void aRecordWithoutDataIsDeletedInATransaction() {
            when(employeeDao.findById(47)).thenReturn(Optional.of(Fixtures.employeeRecord(47, "EMP0120")));
            when(employeeDao.dependencies(47)).thenReturn(List.of());

            service.delete(47);

            verify(employeeDao).delete(47);
            assertThat(tx.started()).isEqualTo(1);
            verify(audit).record(eq("EMPLOYEE_DELETED"), eq("Employee"), eq(47L), any());
        }
    }

    @Nested
    class DataScope {

        private final EmployeeFilter all = new EmployeeFilter(null, null, null, null, null);
        private final PageRequest page = new PageRequest(1, 10, null, true);

        @Test
        void hrSearchesEveryoneWithSalaries() {
            Employee e = Fixtures.employeeRecord(16, "EMP0016");
            when(employeeDao.search(all, page)).thenReturn(new Page<>(List.of(e), 1, 10, 1));

            assertThat(service.search(Fixtures.hr(5), all, page).items().get(0).getBaseSalary()).isNotNull();
        }

        @Test
        void managersSearchTheirTeamWithoutConfidentialData() {
            when(employeeDao.search(all.withTeamOf(11L), page))
                    .thenReturn(new Page<>(List.of(Fixtures.employeeRecord(16, "EMP0016")), 1, 10, 1));

            Employee e = service.search(Fixtures.manager(11), all, page).items().get(0);

            assertThat(e.getBaseSalary()).isNull();
            assertThat(e.getNationalId()).isNull();
            assertThat(e.getBankAccount()).isNull();
            assertThat(e.getFullName()).isNotNull();
        }

        @Test
        void employeesCannotSearch() {
            assertThatThrownBy(() -> service.search(Fixtures.employee(16), all, page)).isInstanceOf(ForbiddenException.class);
        }

        @Test
        void aManagerSeesOnlyMaskedProfilesOfHisTeam() {
            when(employeeDao.findById(16)).thenReturn(Optional.of(Fixtures.employeeRecord(16, "EMP0016")));
            when(employeeDao.isInTeam(11, 16)).thenReturn(true);
            when(employeeDao.isInTeam(12, 16)).thenReturn(false);

            assertThat(service.get(Fixtures.manager(11), 16).getBaseSalary()).isNull();
            assertThatThrownBy(() -> service.get(Fixtures.manager(12), 16)).isInstanceOf(ForbiddenException.class);
        }

        @Test
        void everyoneSeesHisOwnFullProfileButNotOthers() {
            when(employeeDao.findById(16)).thenReturn(Optional.of(Fixtures.employeeRecord(16, "EMP0016")));

            assertThat(service.get(Fixtures.employee(16), 16).getBaseSalary()).isNotNull();
            assertThatThrownBy(() -> service.get(Fixtures.employee(17), 16)).isInstanceOf(ForbiddenException.class);
        }

        @Test
        void lookupByCodeIsCaseInsensitiveAndScoped() {
            when(employeeDao.findByCode("EMP0016")).thenReturn(Optional.of(Fixtures.employeeRecord(16, "EMP0016")));
            when(employeeDao.findById(16)).thenReturn(Optional.of(Fixtures.employeeRecord(16, "EMP0016")));

            assertThat(service.getByCode(Fixtures.technical(), " emp0016 ").getEmployeeCode()).isEqualTo("EMP0016");
            assertThatThrownBy(() -> service.getByCode(Fixtures.employee(17), "EMP0016")).isInstanceOf(ForbiddenException.class);
        }

        @Test
        void salaryHistoryIsForHrAndTheEmployeeHimself() {
            service.salaryHistory(Fixtures.employee(16), 16);
            service.salaryHistory(Fixtures.hr(5), 16);

            assertThatThrownBy(() -> service.salaryHistory(Fixtures.manager(11), 16)).isInstanceOf(ForbiddenException.class);
            verify(employeeDao, times(2)).salaryHistory(16);
        }
    }

    @Test
    void nextCodeUsesTheEmpPrefix() {
        when(employeeDao.nextCode("EMP")).thenReturn("EMP0121");

        assertThat(service.nextCode()).isEqualTo("EMP0121");
    }
}
