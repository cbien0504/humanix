package com.hrm.report;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV exporter (RFC 4180 via Apache Commons CSV). Writes a UTF-8 BOM so Excel shows Vietnamese
 * correctly, and neutralises CSV/formula injection: a text cell starting with = + - @ (or tab/CR)
 * is prefixed with an apostrophe, so a name like "=HYPERLINK(...)" is never executed by Excel.
 */
public class CsvReportExporter implements ReportExporter {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public String contentType() {
        return "text/csv;charset=UTF-8";
    }

    @Override
    public String extension() {
        return "csv";
    }

    @Override
    public void write(TabularReport report, OutputStream out) throws IOException {
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        writer.write('﻿');
        CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT);
        List<String> headers = new ArrayList<>();
        report.columns().forEach(c -> headers.add(c.header()));
        csv.printRecord(headers);
        for (List<Object> row : report.rows()) {
            List<String> cells = new ArrayList<>(row.size());
            for (Object value : row) {
                cells.add(format(value));
            }
            csv.printRecord(cells);
        }
        csv.flush();
    }

    static String format(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal bd) {
            return bd.stripTrailingZeros().toPlainString();
        }
        if (value instanceof Number n) {
            return n.toString();             // numbers (also negative ones) are never sanitised
        }
        if (value instanceof LocalDate d) {
            return DMY.format(d);
        }
        return sanitize(value.toString());
    }

    /** Prevents spreadsheet formula injection (OWASP "CSV Injection"). */
    static String sanitize(String text) {
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            return "'" + text;
        }
        return text;
    }
}
