package com.hrm.web;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatting functions exposed to JSP as EL functions ({@code /WEB-INF/hrm.tld}, prefix {@code h}):
 * {@code ${h:date(e.hireDate)}}, {@code ${h:money(p.netSalary)}}… JSTL's fmt tags do not support
 * java.time types. Output still goes through {@code <c:out>} for escaping.
 */
public final class Formats {

    private static final Locale VI = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static volatile ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");

    private Formats() {
    }

    public static void setZone(ZoneId companyZone) {
        zone = companyZone;
    }

    public static String date(LocalDate value) {
        return value == null ? "" : DATE.format(value);
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    public static String time(LocalDateTime value) {
        return value == null ? "" : TIME.format(value);
    }

    /** UTC instant shown in the company time zone. */
    public static String instant(Instant value) {
        return value == null ? "" : DATE_TIME.format(value.atZone(zone));
    }

    public static String money(Number value) {
        if (value == null) {
            return "";
        }
        NumberFormat nf = NumberFormat.getIntegerInstance(VI);
        return nf.format(value instanceof BigDecimal bd ? bd.setScale(0, java.math.RoundingMode.HALF_UP) : value);
    }

    public static String number(Number value) {
        if (value == null) {
            return "";
        }
        NumberFormat nf = NumberFormat.getNumberInstance(VI);
        nf.setMaximumFractionDigits(2);
        return nf.format(value);
    }
}
