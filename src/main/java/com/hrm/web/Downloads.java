package com.hrm.web;

import com.hrm.report.ReportExporter;
import com.hrm.report.TabularReport;

import java.io.IOException;
import java.io.OutputStream;

public final class Downloads {

    private Downloads() {
    }

    public static void report(WebContext ctx, TabularReport report, String baseFileName) throws IOException {
        ReportExporter exporter = ReportExporter.forFormat(ctx.param("format"));
        try (OutputStream out = ctx.download(baseFileName + "." + exporter.extension(), exporter.contentType())) {
            exporter.write(report, out);
        }
    }
}
