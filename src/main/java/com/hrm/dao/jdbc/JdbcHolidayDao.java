package com.hrm.dao.jdbc;

import com.hrm.dao.HolidayDao;

import java.time.LocalDate;
import java.util.List;

public class JdbcHolidayDao implements HolidayDao {

    private final JdbcExecutor jdbc;

    public JdbcHolidayDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<LocalDate> findBetween(LocalDate from, LocalDate to) {
        return jdbc.query("SELECT HolidayDate FROM dbo.Holidays WHERE HolidayDate BETWEEN ? AND ?",
                rs -> Rows.date(rs, "HolidayDate"), from, to);
    }
}
