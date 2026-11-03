package com.hrm.dao.jdbc;

import com.hrm.model.PageRequest;

import java.util.Map;

public final class SqlOrder {

    private SqlOrder() {
    }

    public static String of(Map<String, String> whitelist, PageRequest page, String defaultColumn, String tieBreaker) {
        String column = page.sort() == null ? null : whitelist.get(page.sort());
        return " ORDER BY " + (column == null ? defaultColumn : column) + (page.ascending() ? " ASC" : " DESC")
                + ", " + tieBreaker;
    }
}
