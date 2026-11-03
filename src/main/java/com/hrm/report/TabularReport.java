package com.hrm.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TabularReport {

    private final String title;
    private final List<String> infoLines;
    private final List<ReportColumn> columns;
    private final List<List<Object>> rows = new ArrayList<>();

    public TabularReport(String title, List<String> infoLines, List<ReportColumn> columns) {
        this.title = title;
        this.infoLines = List.copyOf(infoLines);
        this.columns = List.copyOf(columns);
    }

    public TabularReport addRow(Object... values) {
        if (values.length != columns.size()) {
            throw new IllegalArgumentException("Expected " + columns.size() + " values, got " + values.length);
        }
        List<Object> row = new ArrayList<>(values.length);
        Collections.addAll(row, values);
        rows.add(row);
        return this;
    }

    public String title() {
        return title;
    }

    public List<String> infoLines() {
        return infoLines;
    }

    public List<ReportColumn> columns() {
        return columns;
    }

    public List<List<Object>> rows() {
        return Collections.unmodifiableList(rows);
    }

    public boolean hasTotals() {
        return columns.stream().anyMatch(ReportColumn::total);
    }
}
