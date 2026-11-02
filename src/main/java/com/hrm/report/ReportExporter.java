package com.hrm.report;

import java.io.IOException;
import java.io.OutputStream;

/** Writes a {@link TabularReport} in one file format. */
public interface ReportExporter {

    String contentType();

    String extension();

    void write(TabularReport report, OutputStream out) throws IOException;

    /** Exporter for {@code "xlsx"} (default) or {@code "csv"}. */
    static ReportExporter forFormat(String format) {
        return "csv".equalsIgnoreCase(format) ? new CsvReportExporter() : new ExcelReportExporter();
    }
}
