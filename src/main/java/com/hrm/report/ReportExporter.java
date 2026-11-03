package com.hrm.report;

import java.io.IOException;
import java.io.OutputStream;

public interface ReportExporter {

    String contentType();

    String extension();

    void write(TabularReport report, OutputStream out) throws IOException;

    static ReportExporter forFormat(String format) {
        return "csv".equalsIgnoreCase(format) ? new CsvReportExporter() : new ExcelReportExporter();
    }
}
