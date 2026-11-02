package com.hrm.dao;

import java.time.LocalDate;
import java.util.List;

public interface HolidayDao {

    List<LocalDate> findBetween(LocalDate from, LocalDate to);
}
