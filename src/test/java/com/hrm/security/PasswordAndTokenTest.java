package com.hrm.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** BCrypt hashing (passwords are never stored in clear) and CSRF token comparison. */
class PasswordAndTokenTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher(4);     // minimum cost keeps the test fast

    @Test
    void hashesAreSaltedAndVerifiable() {
        String first = hasher.hash("Demo@123");
        String second = hasher.hash("Demo@123");

        assertThat(first).startsWith("$2a$04$").doesNotContain("Demo@123").isNotEqualTo(second);
        assertThat(hasher.matches("Demo@123", first)).isTrue();
        assertThat(hasher.matches("demo@123", first)).isFalse();
    }

    @Test
    void malformedOrMissingHashesNeverMatch() {
        assertThat(hasher.matches("Demo@123", "plain-text")).isFalse();
        assertThat(hasher.matches("Demo@123", "$2a$10$tooShort")).isFalse();
        assertThat(hasher.matches(null, "$2a$04$x")).isFalse();
        assertThat(hasher.matches("Demo@123", null)).isFalse();
    }

    @Test
    void costIsValidated() {
        assertThatThrownBy(() -> new BCryptPasswordHasher(3)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void passwordPolicyRequiresLengthAndMixedCharacters() {
        PasswordPolicy policy = new PasswordPolicy(8);

        assertThat(policy.check("Ab1", "hr")).contains("ít nhất 8");
        assertThat(policy.check("abcdefgh1", "hr")).contains("chữ hoa");
        assertThat(policy.check("Nguyen2026", "nguyen2026")).contains("trùng tên đăng nhập");
        assertThat(policy.check("Nguyen2026", "hr")).isNull();
        for (int i = 0; i < 20; i++) {
            String temporary = policy.generateTemporary();
            assertThat(policy.check(temporary, "hr")).as(temporary).isNull();
        }
    }

    @Test
    void csrfTokensCompareExactly() {
        assertThat(CsrfTokens.matches("abc123", "abc123")).isTrue();
        assertThat(CsrfTokens.matches("abc123", "abc124")).isFalse();
        assertThat(CsrfTokens.matches("abc123", null)).isFalse();
        assertThat(CsrfTokens.matches(null, "abc123")).isFalse();
    }
}
