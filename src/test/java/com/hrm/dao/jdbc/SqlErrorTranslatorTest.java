package com.hrm.dao.jdbc;

import com.hrm.exception.BusinessException;
import com.hrm.exception.DataAccessException;
import com.hrm.exception.DataIntegrityException;
import com.hrm.exception.DatabaseUnavailableException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;

import static org.assertj.core.api.Assertions.assertThat;

/** SQL Server error → application exception with a user-friendly message (no raw SQL shown to users). */
class SqlErrorTranslatorTest {

    private final SqlErrorTranslator translator = new SqlErrorTranslator();

    private RuntimeException translate(String message, String sqlState, int errorCode) {
        return translator.translate(new SQLException(message, sqlState, errorCode), "INSERT INTO dbo.Employees (...) VALUES (?)");
    }

    @Test
    void duplicateEmployeeCodeBecomesA409OnTheCodeField() {
        RuntimeException e = translate("Violation of UNIQUE KEY constraint 'UQ_Employees_EmployeeCode'. Cannot insert duplicate key "
                + "in object 'dbo.Employees'. The duplicate key value is (EMP0001).", "23000", 2627);

        assertThat(e).isInstanceOfSatisfying(DataIntegrityException.class, d -> {
            assertThat(d.getHttpStatus()).isEqualTo(409);
            assertThat(d.getField()).isEqualTo("employeeCode");
            assertThat(d.getMessage()).isEqualTo("Mã nhân viên đã tồn tại.");
        });
    }

    @Test
    void uniqueIndexViolationsAreRecognisedToo() {
        RuntimeException e = translate("Cannot insert duplicate key row in object 'dbo.Employees' with unique index "
                + "'UX_Employees_NationalId'. The duplicate key value is (001090000001).", "23000", 2601);

        assertThat(e).isInstanceOfSatisfying(DataIntegrityException.class, d -> assertThat(d.getField()).isEqualTo("nationalId"));
    }

    @Test
    void negativeSalaryStoppedByTheCheckConstraintPointsToTheSalaryField() {
        RuntimeException e = translate("The INSERT statement conflicted with the CHECK constraint \"CK_Employees_Salary\". The "
                + "conflict occurred in database \"HRM\", table \"dbo.Employees\".", "23000", 547);

        assertThat(e).isInstanceOfSatisfying(DataIntegrityException.class, d -> {
            assertThat(d.getField()).isEqualTo("baseSalary");
            assertThat(d.getMessage()).isEqualTo("Lương cơ bản và phụ cấp không được âm.");
        });
    }

    @Test
    void foreignKeyOnDeleteExplainsWhatToDoInstead() {
        RuntimeException e = translate("The DELETE statement conflicted with the REFERENCE constraint \"FK_Attendance_Employees\".",
                "23000", 547);

        assertThat(e.getMessage()).contains("chuyển sang trạng thái Nghỉ việc");
    }

    @Test
    void unknownConstraintsGetAGenericMessageWithTheirName() {
        assertThat(translate("Violation of UNIQUE KEY constraint 'UQ_New_Thing'.", "23000", 2627).getMessage())
                .isEqualTo("Dữ liệu bị trùng lặp (UQ_New_Thing).");
        assertThat(translate("conflict without a name", "23000", 547).getMessage())
                .isEqualTo("Dữ liệu vi phạm ràng buộc toàn vẹn.");
    }

    @Test
    void businessErrorsThrownByTriggersReachTheUserAsTheyAre() {
        RuntimeException e = translate("Nhân viên đã có đơn nghỉ phép trùng thời gian.", "S0001", 51001);

        assertThat(e).isInstanceOf(BusinessException.class).hasMessage("Nhân viên đã có đơn nghỉ phép trùng thời gian.");
    }

    @Test
    void deadlockVictimIsAskedToRetry() {
        assertThat(translate("Transaction was deadlocked on lock resources", "40001", 1205))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("thử lại");
    }

    @Test
    void connectionFailuresMean503() {
        assertThat(translate("Connection reset", "08S01", 0)).isInstanceOf(DatabaseUnavailableException.class);

        SQLException wrapped = new SQLException("pool exhausted", null, 0,
                new SQLTransientConnectionException("hrm-pool - Connection is not available, request timed out after 5000ms."));
        assertThat(translator.translate(wrapped, "SELECT 1")).isInstanceOf(DatabaseUnavailableException.class);
    }

    @Test
    void anythingElseKeepsAnAbbreviatedStatementForTheLogOnly() {
        String longSql = "SELECT " + "x, ".repeat(100) + "y FROM dbo.Employees";

        RuntimeException e = translator.translate(new SQLException("Invalid column name 'y'.", "S0001", 207), longSql);

        assertThat(e).isInstanceOf(DataAccessException.class);
        assertThat(e.getMessage()).contains("[207]").endsWith("…").hasSizeLessThan(200);
    }

    @Test
    void constraintNameIsReadFromEitherQuoteStyle() {
        assertThat(SqlErrorTranslator.constraintName("... constraint \"CK_A\" ...")).isEqualTo("CK_A");
        assertThat(SqlErrorTranslator.constraintName("... index 'UX_B' ...")).isEqualTo("UX_B");
        assertThat(SqlErrorTranslator.constraintName(null)).isNull();
    }
}
