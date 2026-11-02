package com.hrm.dao.jdbc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tiny builder for dynamic WHERE clauses. Conditions are SQL fragments written by developers
 * with {@code ?} placeholders; user input only ever travels as bound parameters, so search
 * filters stay safe from SQL injection (NFR-02).
 */
public final class SqlWhere {

    private final List<String> conditions = new ArrayList<>();
    private final List<Object> params = new ArrayList<>();

    /** Adds a condition unconditionally. */
    public SqlWhere and(String condition, Object... values) {
        conditions.add(condition);
        Collections.addAll(params, values);
        return this;
    }

    /** Adds the condition only when {@code apply} is true. */
    public SqlWhere andIf(boolean apply, String condition, Object... values) {
        return apply ? and(condition, values) : this;
    }

    /** Adds {@code condition} bound to {@code value} unless the value is null or a blank string. */
    public SqlWhere andIfPresent(String condition, Object value) {
        boolean present = value != null && !(value instanceof String s && s.isBlank());
        return andIf(present, condition, value);
    }

    /**
     * Case/accent-insensitive "contains" search across columns (the database collation is
     * Latin1_General_100_CI_AI). LIKE wildcards typed by the user are escaped.
     */
    public SqlWhere andContains(String keyword, String... columns) {
        if (keyword == null || keyword.isBlank()) {
            return this;
        }
        String pattern = "%" + escapeLike(keyword.trim()) + "%";
        List<String> parts = new ArrayList<>();
        for (String column : columns) {
            parts.add(column + " LIKE ?");
            params.add(pattern);
        }
        conditions.add("(" + String.join(" OR ", parts) + ")");
        return this;
    }

    /** Escapes LIKE metacharacters with SQL Server bracket syntax. */
    public static String escapeLike(String input) {
        return input.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]");
    }

    public String toSql() {
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    public List<Object> params() {
        return Collections.unmodifiableList(params);
    }

    /** Parameters of this clause followed by {@code extra} (e.g. OFFSET / FETCH values). */
    public Object[] paramsWith(Object... extra) {
        List<Object> all = new ArrayList<>(params);
        Collections.addAll(all, extra);
        return all.toArray();
    }
}
