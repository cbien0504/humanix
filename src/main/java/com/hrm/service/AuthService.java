package com.hrm.service;

import com.hrm.dao.UserDao;
import com.hrm.exception.UnauthorizedException;
import com.hrm.exception.ValidationException;
import com.hrm.model.User;
import com.hrm.security.AuthUser;
import com.hrm.security.PasswordHasher;
import com.hrm.security.PasswordPolicy;
import com.hrm.util.Validator;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * Authentication use cases (UC-01, UC-03).
 * <ul>
 *   <li>Unknown user and wrong password produce the same message; a dummy BCrypt check keeps
 *       response times equal so usernames cannot be enumerated by timing.</li>
 *   <li>After {@code maxFailedLogins} consecutive failures the account is locked for
 *       {@code lockMinutes}; while locked the password is not even checked.</li>
 *   <li>The account status (disabled) is only revealed to someone who knows the password.</li>
 * </ul>
 */
public class AuthService {

    static final String BAD_CREDENTIALS = "Tên đăng nhập hoặc mật khẩu không đúng.";
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final UserDao userDao;
    private final PasswordHasher hasher;
    private final PasswordPolicy policy;
    private final AuditService audit;
    private final Clock clock;
    private final int maxFailedLogins;
    private final Duration lockDuration;
    /** Real hash of a random string, used to spend the same time when the username is unknown. */
    private final String dummyHash;

    public AuthService(UserDao userDao, PasswordHasher hasher, PasswordPolicy policy, AuditService audit, Clock clock,
                       int maxFailedLogins, int lockMinutes) {
        this.userDao = userDao;
        this.hasher = hasher;
        this.policy = policy;
        this.audit = audit;
        this.clock = clock;
        this.maxFailedLogins = maxFailedLogins;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
        this.dummyHash = hasher.hash(UUID.randomUUID().toString());
    }

    /** Result of a successful login. */
    public record LoginResult(AuthUser user, boolean mustChangePassword) {
    }

    public LoginResult login(String username, String password) {
        String name = Validator.clean(username);
        if (name == null || password == null || password.isEmpty()) {
            throw new UnauthorizedException("Vui lòng nhập tên đăng nhập và mật khẩu.");
        }
        Instant now = clock.instant();
        Optional<User> found = userDao.findByUsername(name);
        if (found.isEmpty()) {
            hasher.matches(password, dummyHash);
            audit.record(name, "LOGIN_FAILED", "User", null, "Tài khoản không tồn tại");
            throw new UnauthorizedException(BAD_CREDENTIALS);
        }
        User user = found.get();
        if (user.isLocked(now)) {
            audit.record(user.getUsername(), "LOGIN_BLOCKED", "User", user.getUserId(), "Tài khoản đang bị khóa tạm thời");
            throw new UnauthorizedException(lockedMessage(user.getLockedUntil()));
        }
        if (!hasher.matches(password, user.getPasswordHash())) {
            Instant lockUntil = now.plus(lockDuration);
            int failures = userDao.recordLoginFailure(user.getUserId(), maxFailedLogins, lockUntil);
            audit.record(user.getUsername(), "LOGIN_FAILED", "User", user.getUserId(), "Sai mật khẩu lần " + failures);
            if (failures >= maxFailedLogins) {
                audit.record(user.getUsername(), "ACCOUNT_LOCKED", "User", user.getUserId(), "Khóa " + lockDuration.toMinutes() + " phút");
                throw new UnauthorizedException(lockedMessage(lockUntil));
            }
            throw new UnauthorizedException(BAD_CREDENTIALS);
        }
        if (!user.isActive()) {
            audit.record(user.getUsername(), "LOGIN_FAILED", "User", user.getUserId(), "Tài khoản bị vô hiệu hóa");
            throw new UnauthorizedException("Tài khoản đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên.");
        }
        userDao.recordLoginSuccess(user.getUserId(), now);
        audit.record(user.getUsername(), "LOGIN_SUCCESS", "User", user.getUserId(), null);
        String displayName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        AuthUser principal = new AuthUser(user.getUserId(), user.getUsername(), user.getEmployeeId(), displayName, user.getRoles());
        return new LoginResult(principal, user.isMustChangePassword());
    }

    public void changePassword(AuthUser principal, String currentPassword, String newPassword, String confirmPassword) {
        User user = userDao.findById(principal.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Phiên đăng nhập không hợp lệ."));
        Validator v = new Validator()
                .required("currentPassword", currentPassword, "Vui lòng nhập mật khẩu hiện tại.")
                .required("newPassword", newPassword, "Vui lòng nhập mật khẩu mới.");
        v.throwIfInvalid();
        if (!hasher.matches(currentPassword, user.getPasswordHash())) {
            throw new ValidationException("currentPassword", "Mật khẩu hiện tại không đúng.");
        }
        String violation = policy.check(newPassword, user.getUsername());
        v.check(violation == null, "newPassword", violation)
                .check(!newPassword.equals(currentPassword), "newPassword", "Mật khẩu mới phải khác mật khẩu hiện tại.")
                .check(newPassword.equals(confirmPassword), "confirmPassword", "Xác nhận mật khẩu không khớp.")
                .throwIfInvalid();
        userDao.updatePassword(user.getUserId(), hasher.hash(newPassword), false);
        audit.record(user.getUsername(), "PASSWORD_CHANGED", "User", user.getUserId(), null);
    }

    private String lockedMessage(Instant until) {
        String time = HH_MM.format(until.atZone(clock.getZone()));
        return "Tài khoản tạm khóa do đăng nhập sai " + maxFailedLogins + " lần. Vui lòng thử lại sau " + time + ".";
    }
}
