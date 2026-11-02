package com.hrm.report;

/** Column definition; {@code total = true} adds a SUM to the totals row (numeric columns only). */
public record ReportColumn(String header, ColumnType type, int width, boolean total) {

    public static ReportColumn text(String header, int width) {
        return new ReportColumn(header, ColumnType.TEXT, width, false);
    }

    public static ReportColumn integer(String header) {
        return new ReportColumn(header, ColumnType.INTEGER, 10, false);
    }

    public static ReportColumn decimal(String header) {
        return new ReportColumn(header, ColumnType.DECIMAL, 11, false);
    }

    public static ReportColumn money(String header) {
        return new ReportColumn(header, ColumnType.MONEY, 15, true);
    }

    public static ReportColumn percent(String header) {
        return new ReportColumn(header, ColumnType.PERCENT, 10, false);
    }

    public static ReportColumn date(String header) {
        return new ReportColumn(header, ColumnType.DATE, 12, false);
    }

    public ReportColumn withTotal() {
        return new ReportColumn(header, type, width, true);
    }

    /** For averages and rates, where a SUM row would be meaningless. */
    public ReportColumn noTotal() {
        return new ReportColumn(header, type, width, false);
    }
}
