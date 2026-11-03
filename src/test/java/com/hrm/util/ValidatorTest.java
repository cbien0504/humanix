package com.hrm.util;

import com.hrm.exception.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidatorTest {

    private final Locale originalLocale = Locale.getDefault();

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    @Test
    void collectsTheFirstMessagePerFieldAndThrowsOnce() {
        Validator v = new Validator()
                .required("fullName", "  ", "Vui lòng nhập họ tên.")
                .maxLength("fullName", "x".repeat(200), 150)            // second failure of the same field is ignored
                .email("email", "abc@")
                .nonNegative("baseSalary", new BigDecimal("-1"), "Lương cơ bản không được âm.")
                .range("dependents", 21, 0, 20, "Số người phụ thuộc từ 0 đến 20.")
                .notBefore("endDate", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), "Sai thứ tự ngày.");

        assertThat(v.hasError("fullName")).isTrue();
        assertThatThrownBy(v::throwIfInvalid)
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getFieldErrors())
                        .containsEntry("fullName", "Vui lòng nhập họ tên.")
                        .containsEntry("email", "Email không hợp lệ.")
                        .containsEntry("baseSalary", "Lương cơ bản không được âm.")
                        .containsKeys("dependents", "endDate")
                        .hasSize(5));
    }

    @Test
    void optionalValuesAreValidWhenAbsent() {
        new Validator()
                .email("email", null)
                .phone("phone", "")
                .pattern("nationalId", null, Validator.NATIONAL_ID, "CCCD")
                .nonNegative("allowance", null, "x")
                .range("dependents", null, 0, 20, "x")
                .notBefore("endDate", null, LocalDate.now(), "x")
                .throwIfInvalid();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0912345678", "+84912345678"})
    void acceptsVietnamesePhoneNumbers(String phone) {
        assertThat(new Validator().phone("phone", phone).hasErrors()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "912345678", "09123456789", "+8491234567", "0912-345-678"})
    void rejectsMalformedPhoneNumbers(String phone) {
        assertThat(new Validator().phone("phone", phone).hasError("phone")).isTrue();
    }

    @Test
    void cleanTrimsAndTurnsBlankIntoNull() {
        assertThat(Validator.clean("  Nguyễn Văn A  ")).isEqualTo("Nguyễn Văn A");
        assertThat(Validator.clean("   ")).isNull();
        assertThat(Validator.clean(null)).isNull();
        assertThat(Validator.upper(" emp0010 ")).isEqualTo("EMP0010");
        assertThat(Validator.lower(" Hung.NV@Example.COM ")).isEqualTo("hung.nv@example.com");
    }

    /**
     * Regression: String.toLowerCase() without a locale follows the JVM default. On a server whose
     * locale is Turkish, "I" lower-cases to dotless "ı" – e-mails and usernames would be stored
     * corrupted ("admın@…") and logins would stop matching.
     */
    @Test
    void caseConversionDoesNotDependOnTheServerLocale() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));

        assertThat(Validator.lower("ADMIN@EXAMPLE.COM")).isEqualTo("admin@example.com");
        assertThat(Validator.upper("linkedin")).isEqualTo("LINKEDIN");
    }
}
