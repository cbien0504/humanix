package com.hrm.dao.jdbc;

import com.hrm.model.PageRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SqlBuildersTest {

    private static final Map<String, String> SORT = Map.of("name", "e.FullName", "salary", "e.BaseSalary");

    @Test
    void optionalConditionsAreSkippedWhenAbsent() {
        SqlWhere where = new SqlWhere()
                .andIfPresent("e.DepartmentId = ?", 4L)
                .andIfPresent("e.Status = ?", "  ")
                .andIfPresent("e.PositionId = ?", null)
                .andIf(false, "1 = 0");

        assertThat(where.toSql()).isEqualTo(" WHERE e.DepartmentId = ?");
        assertThat(where.params()).containsExactly(4L);
        assertThat(new SqlWhere().toSql()).isEmpty();
    }

    @Test
    void keywordSearchBindsOnePatternPerColumnAndKeepsTheInputOutOfTheSql() {
        SqlWhere where = new SqlWhere().andContains(" x' OR 1=1 -- ", "e.EmployeeCode", "e.FullName");

        assertThat(where.toSql()).isEqualTo(" WHERE (e.EmployeeCode LIKE ? OR e.FullName LIKE ?)");
        assertThat(where.params()).containsExactly("%x' OR 1=1 --%", "%x' OR 1=1 --%");
        assertThat(where.paramsWith(0, 20)).containsExactly("%x' OR 1=1 --%", "%x' OR 1=1 --%", 0, 20);
    }

    @Test
    void likeWildcardsTypedByTheUserAreSearchedLiterally() {
        assertThat(SqlWhere.escapeLike("50%_[x]")).isEqualTo("50[%][_][[]x]");
        assertThat(new SqlWhere().andContains("", "e.FullName").toSql()).isEmpty();
    }

    @Test
    void orderByUsesOnlyWhitelistedColumns() {
        assertThat(SqlOrder.of(SORT, PageRequest.of(1, 10, "salary", "desc", 10), "e.EmployeeCode", "e.EmployeeId"))
                .isEqualTo(" ORDER BY e.BaseSalary DESC, e.EmployeeId");
        assertThat(SqlOrder.of(SORT, PageRequest.of(1, 10, "salary; DROP TABLE dbo.Employees--", "asc", 10),
                "e.EmployeeCode", "e.EmployeeId"))
                .isEqualTo(" ORDER BY e.EmployeeCode ASC, e.EmployeeId");
    }

    @Test
    void missingSortFallsBackInsteadOfFailingOnMapOfNull() {
        assertThat(SqlOrder.of(SORT, PageRequest.of(null, null, null, null, 10), "e.EmployeeCode", "e.EmployeeId"))
                .isEqualTo(" ORDER BY e.EmployeeCode ASC, e.EmployeeId");
    }

    @Test
    void pagingIsClampedToSaneValues() {
        PageRequest page = PageRequest.of(-3, 100_000, " ", null, 10);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(PageRequest.MAX_SIZE);
        assertThat(page.offset()).isZero();
        assertThat(PageRequest.of(3, 20, null, null, 10).offset()).isEqualTo(40);
    }
}
