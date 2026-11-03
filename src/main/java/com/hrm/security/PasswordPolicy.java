package com.hrm.security;

import java.security.SecureRandom;

public class PasswordPolicy {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "@#$%&*!";
    private final int minLength;
    private final SecureRandom random = new SecureRandom();

    public PasswordPolicy(int minLength) {
        this.minLength = minLength;
    }

    public String check(String password, String username) {
        if (password == null || password.length() < minLength) {
            return "Mật khẩu phải có ít nhất " + minLength + " ký tự.";
        }
        if (password.length() > 100) {
            return "Mật khẩu tối đa 100 ký tự.";
        }
        if (!password.chars().anyMatch(Character::isUpperCase)
                || !password.chars().anyMatch(Character::isLowerCase)
                || !password.chars().anyMatch(Character::isDigit)) {
            return "Mật khẩu phải có chữ hoa, chữ thường và chữ số.";
        }
        if (username != null && password.equalsIgnoreCase(username)) {
            return "Mật khẩu không được trùng tên đăng nhập.";
        }
        return null;
    }

    public String generateTemporary() {
        String all = UPPER + LOWER + DIGITS + SYMBOLS;
        char[] chars = new char[12];
        chars[0] = UPPER.charAt(random.nextInt(UPPER.length()));
        chars[1] = LOWER.charAt(random.nextInt(LOWER.length()));
        chars[2] = DIGITS.charAt(random.nextInt(DIGITS.length()));
        chars[3] = SYMBOLS.charAt(random.nextInt(SYMBOLS.length()));
        for (int i = 4; i < chars.length; i++) {
            chars[i] = all.charAt(random.nextInt(all.length()));
        }
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char t = chars[i];
            chars[i] = chars[j];
            chars[j] = t;
        }
        return new String(chars);
    }
}
