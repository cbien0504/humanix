package com.hrm.dao.jdbc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SqlWhere {

    private final List<String> conditions = new ArrayList<>();
    private final List<Object> params = new ArrayList<>();

    public SqlWhere and(String condition, Object... values) {
        conditions.add(condition);
        Collections.addAll(params, values);
        return this;
    }

    public SqlWhere andIf(boolean apply, String condition, Object... values) {
        return apply ? and(condition, values) : this;
    }

    public SqlWhere andIfPresent(String condition, Object value) {
        boolean present = value != null && !(value instanceof String s && s.isBlank());
        return andIf(present, condition, value);
    }

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

    public static String escapeLike(String input) {
        return input.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]");
    }

    public String toSql() {
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    public List<Object> params() {
        return Collections.unmodifiableList(params);
    }

    public Object[] paramsWith(Object... extra) {
        List<Object> all = new ArrayList<>(params);
        Collections.addAll(all, extra);
        return all.toArray();
    }
}
