package com.hrm.report;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Excel (.xlsx) exporter on Apache POI's streaming workbook (SXSSF – constant memory for large
 * sheets): title, info lines, styled header with auto-filter, number formats per column type,
 * frozen header and a totals row using SUM formulas (the numbers stay live if HR edits cells).
 */
public class ExcelReportExporter implements ReportExporter {

    @Override
    public String contentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public String extension() {
        return "xlsx";
    }

    @Override
    public void write(TabularReport report, OutputStream out) throws IOException {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("Report");
            DataFormat df = wb.createDataFormat();
            Map<ColumnType, CellStyle> styles = dataStyles(wb, df);
            List<ReportColumn> columns = report.columns();
            int lastCol = columns.size() - 1;

            int r = 0;
            Row titleRow = sheet.createRow(r++);
            Cell title = titleRow.createCell(0);
            title.setCellValue(report.title());
            title.setCellStyle(titleStyle(wb));
            titleRow.setHeightInPoints(24);
            if (lastCol > 0) {
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastCol));
            }
            for (String line : report.infoLines()) {
                sheet.createRow(r++).createCell(0).setCellValue(line);
            }
            r++;

            int headerRowIndex = r;
            Row header = sheet.createRow(r++);
            header.setHeightInPoints(30);
            CellStyle headerStyle = headerStyle(wb);
            for (int c = 0; c < columns.size(); c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(columns.get(c).header());
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(c, Math.min(columns.get(c).width(), 60) * 256);
            }

            int firstDataRow = r;
            for (List<Object> values : report.rows()) {
                Row row = sheet.createRow(r++);
                for (int c = 0; c < columns.size(); c++) {
                    setValue(row.createCell(c), values.get(c), styles.get(columns.get(c).type()));
                }
            }
            int lastDataRow = r - 1;

            if (report.hasTotals() && !report.rows().isEmpty()) {
                Row totals = sheet.createRow(r);
                CellStyle totalStyle = totalStyle(wb, df);
                for (int c = 0; c < columns.size(); c++) {
                    Cell cell = totals.createCell(c);
                    cell.setCellStyle(totalStyle);
                    if (c == 0) {
                        cell.setCellValue("Tổng cộng");
                    } else if (columns.get(c).total()) {
                        String col = CellReference.convertNumToColString(c);
                        cell.setCellFormula("SUM(" + col + (firstDataRow + 1) + ":" + col + (lastDataRow + 1) + ")");
                    }
                }
            }
            sheet.createFreezePane(0, headerRowIndex + 1);
            sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, Math.max(headerRowIndex, lastDataRow), 0, lastCol));
            wb.write(out);
            wb.dispose();
        }
    }

    private static void setValue(Cell cell, Object value, CellStyle style) {
        cell.setCellStyle(style);
        if (value == null) {
            return;
        }
        if (value instanceof BigDecimal bd) {
            cell.setCellValue(bd.doubleValue());
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else if (value instanceof LocalDate d) {
            cell.setCellValue(d);
        } else if (value instanceof LocalDateTime dt) {
            cell.setCellValue(dt);
        } else if (value instanceof Boolean b) {
            cell.setCellValue(b ? "Có" : "Không");
        } else {
            cell.setCellValue(value.toString());   // always a string cell – never interpreted as a formula
        }
    }

    private static Map<ColumnType, CellStyle> dataStyles(SXSSFWorkbook wb, DataFormat df) {
        Map<ColumnType, CellStyle> m = new EnumMap<>(ColumnType.class);
        m.put(ColumnType.TEXT, bordered(wb));
        m.put(ColumnType.INTEGER, withFormat(wb, df, "#,##0"));
        m.put(ColumnType.DECIMAL, withFormat(wb, df, "#,##0.0#"));
        m.put(ColumnType.MONEY, withFormat(wb, df, "#,##0"));
        m.put(ColumnType.PERCENT, withFormat(wb, df, "0.0\"%\""));
        m.put(ColumnType.DATE, withFormat(wb, df, "dd/mm/yyyy"));
        return m;
    }

    private static CellStyle bordered(SXSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private static CellStyle withFormat(SXSSFWorkbook wb, DataFormat df, String format) {
        CellStyle s = bordered(wb);
        s.setDataFormat(df.getFormat(format));
        return s;
    }

    private static CellStyle titleStyle(SXSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        f.setColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFont(f);
        return s;
    }

    private static CellStyle headerStyle(SXSSFWorkbook wb) {
        CellStyle s = bordered(wb);
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setWrapText(true);
        return s;
    }

    private static CellStyle totalStyle(SXSSFWorkbook wb, DataFormat df) {
        CellStyle s = withFormat(wb, df, "#,##0");
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }
}
