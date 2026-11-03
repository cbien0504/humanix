package com.hrm.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt implementation (adaptive, salted). Cost 10 ≈ 60–80 ms per hash on a modern CPU: fast
 * enough for login, slow enough to make offline brute force of a leaked table impractical.
 */
public class BCryptPasswordHasher implements PasswordHasher {

    private final int cost;

    public BCryptPasswordHasher(int cost) {
        if (cost < 4 || cost > 31) {
            throw new IllegalArgumentException("BCrypt cost must be between 4 and 31");
        }
        this.cost = cost;
    }

    @Override
    public String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(cost));
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        if (rawPassword == null || hash == null || !hash.startsWith("$2")) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, hash);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            // malformed or truncated hash: jBCrypt throws StringIndexOutOfBoundsException for "$2a$10$abc"
            return false;
        }
    }
}
