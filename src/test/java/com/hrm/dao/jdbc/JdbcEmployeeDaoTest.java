package com.hrm.dao.jdbc;

import com.hrm.model.Employee;
import com.hrm.model.EmployeeFilter;
import com.hrm.model.EmployeeStatus;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DAO test with a mocked JDBC driver: checks the SQL that reaches the database. A hostile keyword
 * and sort key must never become part of the statement text (SQL injection, NFR-02).
 */
@ExtendWith(MockitoExtension.class)
class JdbcEmployeeDaoTest {

    private static final String HOSTILE = "x' OR 1=1; DROP TABLE dbo.Employees --";

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection con;
    @Mock
    private PreparedStatement ps;
    @Mock
    private ResultSet items;
    @Mock
    private ResultSet count;

    private JdbcEmployeeDao dao;

    @BeforeEach
    void setUp() throws SQLException {
        dao = new JdbcEmployeeDao(new JdbcExecutor(dataSource, () -> "hr", 60_000));
        when(dataSource.getConnection()).thenReturn(con);
        when(con.prepareStatement(anyString())).thenReturn(ps);
    }

    @Test
    void searchSendsOnlyPlaceholdersAndBindsTheUserInput() throws SQLException {
        when(ps.executeQuery()).thenReturn(items, count);
        when(items.next()).thenReturn(false);
        when(count.next()).thenReturn(true, false);
        when(count.getObject(1)).thenReturn(7);

        Page<Employee> page = dao.search(new EmployeeFilter(HOSTILE, null, null, EmployeeStatus.ACTIVE, 11L),
                PageRequest.of(2, 10, "salary; DROP TABLE dbo.Employees--", "desc", 10));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(con, times(2)).prepareStatement(sql.capture());
        String select = flat(sql.getAllValues().get(0));
        String total = flat(sql.getAllValues().get(1));

        assertThat(sql.getAllValues()).noneMatch(s -> s.contains("1=1") || s.contains("DROP"));
        assertThat(select).contains("FROM dbo.vw_EmployeeDetails AS e WHERE (e.EmployeeCode LIKE ? OR e.FullName LIKE ? "
                        + "OR e.Email LIKE ? OR e.Phone LIKE ?) AND e.Status = ? AND e.EmployeeId <> ?")
                .endsWith(" ORDER BY e.EmployeeCode DESC, e.EmployeeId OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        assertThat(total).startsWith("SELECT COUNT(*) FROM dbo.vw_EmployeeDetails AS e WHERE (");

        String pattern = "%" + HOSTILE + "%";
        verify(ps, times(2)).setString(1, pattern);              // once for the page, once for the count
        verify(ps, times(2)).setString(4, pattern);
        verify(ps, times(2)).setString(5, "ACTIVE");
        verify(ps, times(2)).setLong(6, 11L);
        verify(ps).setInt(9, 10);                                // OFFSET of page 2
        verify(ps).setInt(10, 10);                               // FETCH NEXT
        assertThat(page.totalItems()).isEqualTo(7);
        assertThat(page.items()).isEmpty();
    }

    @Test
    void lookupsAreParameterisedToo() throws SQLException {
        when(ps.executeQuery()).thenReturn(items);
        when(items.next()).thenReturn(false);

        assertThat(dao.findByCode("EMP0016' --")).isEmpty();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(con).prepareStatement(sql.capture());
        assertThat(sql.getValue()).endsWith(" WHERE e.EmployeeCode = ?").doesNotContain("EMP0016");
        verify(ps).setString(1, "EMP0016' --");
    }

    private static String flat(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
