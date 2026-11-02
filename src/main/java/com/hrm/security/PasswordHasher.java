package com.hrm.security;

/** One-way password hashing (abstraction so services can be unit-tested without BCrypt cost). */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
