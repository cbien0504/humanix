package com.hrm.dao.jdbc;

import com.hrm.model.PageRequest;

import java.util.Map;

/**
 * Builds a safe ORDER BY clause from a whitelist (sort key → column). Unknown or missing keys
 * fall back to the default column; the tie-breaker keeps OFFSET/FETCH paging deterministic.
 * Note: the whitelist is usually a {@code Map.of(...)}, whose {@code get(null)} throws
 * NullPointerException – hence the explicit null check.
 */
public final class SqlOrder {

    private SqlOrder() {
    }

    public static String of(Map<String, String> whitelist, PageRequest page, String defaultColumn, String tieBreaker) {
        String column = page.sort() == null ? null : whitelist.get(page.sort());
        return " ORDER BY " + (column == null ? defaultColumn : column) + (page.ascending() ? " ASC" : " DESC")
                + ", " + tieBreaker;
    }
}
