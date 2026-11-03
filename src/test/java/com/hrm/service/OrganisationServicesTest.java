package com.hrm.service;

import com.hrm.dao.AuditDao;
import com.hrm.dao.DataQualityDao;
import com.hrm.dao.DepartmentDao;
import com.hrm.dao.EmployeeDao;
import com.hrm.dao.PositionDao;
import com.hrm.exception.BusinessException;
import com.hrm.exception.NotFoundException;
import com.hrm.exception.ValidationException;
import com.hrm.model.Department;
import com.hrm.model.DepartmentForm;
import com.hrm.model.Position;
import com.hrm.model.PositionForm;
import com.hrm.model.ValidationCheck;
import com.hrm.security.UserContext;
import com.hrm.testsupport.Fixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.hrm.testsupport.Fixtures.vnd;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganisationServicesTest {

    @Mock
    private DepartmentDao departmentDao;
    @Mock
    private PositionDao positionDao;
    @Mock
    private EmployeeDao employeeDao;
    @Mock
    private AuditDao auditDao;
    @Mock
    private DataQualityDao dataQualityDao;
    @Mock
    private AuditService audit;

    @AfterEach
    void clearUser() {
        UserContext.clear();
    }

    @Nested
    class Departments {

        private DepartmentService service() {
            return new DepartmentService(departmentDao, employeeDao, audit);
        }

        @Test
        void codesAreUpperCasedAndManagersMustExist() {
            when(employeeDao.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().create(new DepartmentForm("qa team", " ", null, 99L, null)))
                    .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getFieldErrors())
                            .containsKeys("departmentCode", "departmentName", "managerId"));

            when(departmentDao.insert(any())).thenReturn(7L);
            when(departmentDao.findById(7)).thenReturn(Optional.of(Fixtures.department(7, "QA", true)));
            service().create(new DepartmentForm(" qa ", "Phòng Kiểm thử", null, null, null));
            ArgumentCaptor<DepartmentForm> stored = ArgumentCaptor.forClass(DepartmentForm.class);
            verify(departmentDao).insert(stored.capture());
            assertThat(stored.getValue().departmentCode()).isEqualTo("QA");
            assertThat(stored.getValue().active()).isTrue();
        }

        @Test
        void aDepartmentWithEmployeesCanNeitherBeDeactivatedNorDeleted() {
            Department it = Fixtures.department(4, "IT", true);
            it.setEmployeeCount(11);
            when(departmentDao.findById(4)).thenReturn(Optional.of(it));

            assertThatThrownBy(() -> service().update(4, new DepartmentForm("IT", "Phòng CNTT", null, null, false)))
                    .hasMessageContaining("11 nhân viên");
            assertThatThrownBy(() -> service().delete(4)).isInstanceOf(BusinessException.class);
            verify(departmentDao, never()).delete(anyLong());
        }

        @Test
        void anEmptyDepartmentIsDeletedAndAudited() {
            when(departmentDao.findById(7)).thenReturn(Optional.of(Fixtures.department(7, "QA", true)));

            service().delete(7);

            verify(departmentDao).delete(7);
            verify(audit).record("DEPARTMENT_DELETED", "Department", 7L, "QA");
        }

        @Test
        void unknownDepartmentIsNotFound() {
            when(departmentDao.findById(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().get(404)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class Positions {

        private PositionService service() {
            return new PositionService(positionDao, audit);
        }

        @Test
        void salaryBandMustBeOrderedAndNonNegative() {
            assertThatThrownBy(() -> service().create(new PositionForm("DEV", "Lập trình viên", 11, vnd(-1), vnd(5_000_000), null, null)))
                    .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getFieldErrors())
                            .containsKeys("jobLevel", "minSalary"));
            assertThatThrownBy(() -> service().create(new PositionForm("DEV", "Lập trình viên", 3, vnd(28_000_000), vnd(12_000_000), null, null)))
                    .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getFieldErrors())
                            .containsOnlyKeys("maxSalary"));
            verify(positionDao, never()).insert(any());
        }

        @Test
        void defaultsLevelOneAndZeroMinimum() {
            when(positionDao.insert(any())).thenReturn(14L);
            when(positionDao.findById(14)).thenReturn(Optional.of(Fixtures.position(14, "BA", 0, 20_000_000)));

            service().create(new PositionForm(" ba ", "Phân tích nghiệp vụ", null, null, vnd(20_000_000), null, null));

            ArgumentCaptor<PositionForm> stored = ArgumentCaptor.forClass(PositionForm.class);
            verify(positionDao).insert(stored.capture());
            assertThat(stored.getValue().positionCode()).isEqualTo("BA");
            assertThat(stored.getValue().jobLevel()).isEqualTo(1);
            assertThat(stored.getValue().minSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void aPositionInUseCannotBeDeleted() {
            Position dev = Fixtures.position(6, "DEV", 12_000_000, 28_000_000);
            dev.setEmployeeCount(7);
            when(positionDao.findById(6)).thenReturn(Optional.of(dev));

            assertThatThrownBy(() -> service().delete(6)).hasMessageContaining("7 nhân viên");
        }

        @Test
        void updateIsAuditedWithTheBand() {
            when(positionDao.findById(6)).thenReturn(Optional.of(Fixtures.position(6, "DEV", 12_000_000, 28_000_000)));

            service().update(6, new PositionForm("DEV", "Lập trình viên", 4, vnd(13_000_000), vnd(30_000_000), null, true));

            verify(audit).record("POSITION_UPDATED", "Position", 6L, "DEV 13000000-30000000");
        }
    }

    @Nested
    class Auditing {

        @Test
        void recordsTheCurrentUserAndClientAddress() {
            UserContext.set(Fixtures.hr(5));
            UserContext.setClientIp("10.0.0.8");

            new AuditService(auditDao).record("EMPLOYEE_UPDATED", "Employee", 16L, "EMP0016");

            verify(auditDao).insert("hr", "EMPLOYEE_UPDATED", "Employee", "16", "EMP0016", "10.0.0.8");
        }

        @Test
        void aFailingAuditTableNeverBreaksTheBusinessOperation() {
            doThrow(new IllegalStateException("audit table offline")).when(auditDao)
                    .insert(anyString(), anyString(), anyString(), any(), any(), any());

            assertThatNoException().isThrownBy(() -> new AuditService(auditDao).record("system", "LOGIN_FAILED", "User", null, null));
        }

        @Test
        void dataQualityRunCountsFailedChecks() {
            when(dataQualityDao.runValidation()).thenReturn(List.of(
                    new ValidationCheck("DUP_CODE", "Trùng mã nhân viên", "ERROR", 0, "PASS", null),
                    new ValidationCheck("NEG_SALARY", "Lương âm", "ERROR", 1, "FAIL", "EMP0099")));

            List<ValidationCheck> checks = new DataQualityService(dataQualityDao, audit).run();

            assertThat(checks).hasSize(2);
            verify(audit).record("DATA_VALIDATION_RUN", "Database", null, "1 kiểm tra lỗi / 2");
        }
    }
}
