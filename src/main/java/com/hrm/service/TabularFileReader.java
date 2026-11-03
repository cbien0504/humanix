package com.hrm.service;

import com.hrm.exception.ValidationException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.EmptyFileException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

public final class TabularFileReader {

    public record DataRow(int rowNumber, Map<String, String> values) {
        public String get(String column) {
            String v = values.get(column);
            return v == null || v.isBlank() ? null : v.trim();
        }
    }

    public static final int MAX_ROWS = 10_000;

    private TabularFileReader() {
    }

    public static List<DataRow> read(InputStream input, String fileName) throws IOException {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        BufferedInputStream in = new BufferedInputStream(input);
        if (name.endsWith(".xlsx")) {
            in.mark(4);
            byte[] magic = in.readNBytes(2);
            in.reset();
            if (magic.length < 2 || magic[0] != 'P' || magic[1] != 'K') {
                throw new ValidationException("file", "File .xlsx không hợp lệ.");
            }
            return readExcel(in);
        }
        if (name.endsWith(".csv")) {
            return readCsv(in);
        }
        throw new ValidationException("file", "Chỉ hỗ trợ file .csv hoặc .xlsx.");
    }

    static String normalizeHeader(String header) {
        return header == null ? "" : header.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
    }

    private static List<DataRow> readCsv(InputStream in) throws IOException {
        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        String firstLine = text.lines().findFirst().orElse("");
        char delimiter = firstLine.chars().filter(c -> c == ';').count() > firstLine.chars().filter(c -> c == ',').count() ? ';' : ',';
        CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(delimiter).setHeader().setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true).setTrim(true).build();
        int[] lineBreaks = lineBreaks(text);
        List<DataRow> rows = new ArrayList<>();
        try (CSVParser parser = format.parse(new StringReader(text))) {
            List<String> headers = parser.getHeaderNames().stream().map(TabularFileReader::normalizeHeader).toList();
            for (CSVRecord record : parser) {
                if (rows.size() >= MAX_ROWS) {
                    throw new ValidationException("file", "File vượt quá " + MAX_ROWS + " dòng dữ liệu.");
                }
                Map<String, String> values = new LinkedHashMap<>();
                for (int i = 0; i < headers.size() && i < record.size(); i++) {
                    values.put(headers.get(i), record.get(i));
                }
                rows.add(new DataRow(lineOf(text, lineBreaks, record.getCharacterPosition()), values));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ValidationException("file", "File CSV không đúng định dạng: " + e.getMessage());
        }
        return rows;
    }

    private static int[] lineBreaks(String text) {
        return IntStream.range(0, text.length()).filter(i -> text.charAt(i) == '\n').toArray();
    }

    private static int lineOf(String text, int[] lineBreaks, long characterPosition) {
        int position = (int) characterPosition;
        while (position < text.length() && (text.charAt(position) == '\n' || text.charAt(position) == '\r')) {
            position++;
        }
        int i = Arrays.binarySearch(lineBreaks, position);
        return (i >= 0 ? i : -i - 1) + 1;
    }

    private static List<DataRow> readExcel(InputStream in) throws IOException {
        List<DataRow> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return rows;
            }
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                while (headers.size() < cell.getColumnIndex()) {
                    headers.add("");
                }
                headers.add(normalizeHeader(formatter.formatCellValue(cell)));
            }
            for (int r = headerRow.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                boolean empty = true;
                for (int c = 0; c < headers.size(); c++) {
                    String v = cellText(row.getCell(c), formatter);
                    values.put(headers.get(c), v);
                    empty &= v.isBlank();
                }
                if (empty) {
                    continue;
                }
                if (rows.size() >= MAX_ROWS) {
                    throw new ValidationException("file", "File vượt quá " + MAX_ROWS + " dòng dữ liệu.");
                }
                rows.add(new DataRow(r + 1, values));
            }
        } catch (EmptyFileException | NotOfficeXmlFileException e) {
            throw new ValidationException("file", "File Excel không hợp lệ.");
        }
        return rows;
    }

    private static String cellText(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        if (type == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate().toString();
            }
            return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
        if (type == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        return cell.getCellType() == CellType.FORMULA ? cell.getStringCellValue() : formatter.formatCellValue(cell);
    }
}
