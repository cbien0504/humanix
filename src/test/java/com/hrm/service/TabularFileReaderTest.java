package com.hrm.service;

import com.hrm.exception.ValidationException;
import com.hrm.service.TabularFileReader.DataRow;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TabularFileReaderTest {

    private static InputStream text(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void csvWithBomAndFriendlyHeadersIsNormalised() throws IOException {
        List<DataRow> rows = TabularFileReader.read(text("﻿Employee Code,Full Name,Base-Salary\nEMP0101,  Phan Hoàng Anh ,30000000\n"),
                "nhan-vien.CSV");

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).values()).containsOnlyKeys("employee_code", "full_name", "base_salary");
        assertThat(rows.get(0).get("full_name")).isEqualTo("Phan Hoàng Anh");
        assertThat(rows.get(0).rowNumber()).isEqualTo(2);
    }

    @Test
    void semicolonSeparatedFilesFromExcelAreDetected() throws IOException {
        List<DataRow> rows = TabularFileReader.read(text("employee_code;full_name;base_salary\nEMP0101;Anh;30.000.000\n"), "a.csv");

        assertThat(rows.get(0).get("base_salary")).isEqualTo("30.000.000");
    }

    @Test
    void rowNumbersAreFileLinesDespiteMultiLineValuesAndBlankLines() throws IOException {
        String csv = """
                employee_code,full_name,address
                EMP0101,Anh,"Số 1 Láng Hạ
                Đống Đa, Hà Nội"

                EMP0102,Hà,Kim Mã
                """;

        List<DataRow> rows = TabularFileReader.read(text(csv), "a.csv");

        assertThat(rows).extracting(DataRow::rowNumber).containsExactly(2, 5);
        assertThat(rows.get(0).get("address")).isEqualTo("Số 1 Láng Hạ\nĐống Đa, Hà Nội");
    }

    @Test
    void blankCellsReadAsNull() throws IOException {
        DataRow row = TabularFileReader.read(text("employee_code,phone\nEMP0101,   \n"), "a.csv").get(0);

        assertThat(row.get("phone")).isNull();
        assertThat(row.get("not_a_column")).isNull();
    }

    @Test
    void xlsxCellsAreReadAsTextWithIsoDatesAndPlainNumbers() throws IOException {
        byte[] file;
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("NhanVien");
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));
            Row header = sheet.createRow(0);
            String[] names = {"Employee Code", "Hire Date", "Base Salary", "Phone", "Active"};
            for (int i = 0; i < names.length; i++) {
                header.createCell(i).setCellValue(names[i]);
            }
            Row r = sheet.createRow(1);
            r.createCell(0).setCellValue("EMP0111");
            r.createCell(1).setCellValue(LocalDate.of(2026, 11, 20));
            r.getCell(1).setCellStyle(dateStyle);
            r.createCell(2).setCellValue(16_500_000.0);
            r.createCell(3).setCellValue(912_345_678);
            r.createCell(4).setCellValue(true);
            sheet.createRow(2);
            Row last = sheet.createRow(3);
            last.createCell(0).setCellValue("EMP0112");
            last.createCell(2).setCellFormula("15000000+500000");
            wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
            wb.write(out);
            file = out.toByteArray();
        }

        List<DataRow> rows = TabularFileReader.read(new ByteArrayInputStream(file), "import.xlsx");

        assertThat(rows).extracting(DataRow::rowNumber).containsExactly(2, 4);
        DataRow first = rows.get(0);
        assertThat(first.get("hire_date")).isEqualTo("2026-11-20");
        assertThat(first.get("base_salary")).isEqualTo("16500000");
        assertThat(first.get("phone")).isEqualTo("912345678");
        assertThat(first.get("active")).isEqualTo("true");
        assertThat(rows.get(1).get("base_salary")).isEqualTo("15500000");
    }

    @Test
    void anXlsxThatIsNotAZipIsRejected() {
        assertThatThrownBy(() -> TabularFileReader.read(text("employee_code\nEMP1"), "renamed.xlsx"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getFieldErrors()).containsEntry("file", "File .xlsx không hợp lệ."));
    }

    @Test
    void onlyCsvAndXlsxAreAccepted() {
        assertThatThrownBy(() -> TabularFileReader.read(text("x"), "data.xls")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> TabularFileReader.read(text("x"), null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void filesAboveTheRowLimitAreRejected() {
        StringBuilder csv = new StringBuilder("employee_code\n");
        for (int i = 0; i <= TabularFileReader.MAX_ROWS; i++) {
            csv.append("EMP").append(i).append('\n');
        }

        assertThatThrownBy(() -> TabularFileReader.read(text(csv.toString()), "big.csv"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("10000");
    }
}
