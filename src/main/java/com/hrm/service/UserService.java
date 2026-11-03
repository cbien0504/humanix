package com.hrm.service;

import com.hrm.dao.TransactionManager;
import com.hrm.dao.UserDao;
import com.hrm.exception.BusinessException;
import com.hrm.exception.NotFoundException;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.model.User;
import com.hrm.security.AuthUser;
import com.hrm.security.PasswordHasher;
import com.hrm.security.PasswordPolicy;
import com.hrm.security.Role;
import com.hrm.util.Validator;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserService {

    private final UserDao userDao;
    private final PasswordHasher hasher;
    private final PasswordPolicy policy;
    private final AuditService audit;
    private final TransactionManager tx;

    public UserService(UserDao userDao, PasswordHasher hasher, PasswordPolicy policy, AuditService audit, TransactionManager tx) {
        this.userDao = userDao;
        this.hasher = hasher;
        this.policy = policy;
        this.audit = audit;
        this.tx = tx;
    }

    public record CreateUserCommand(String username, Long employeeId, Set<Role> roles) {
    }

    public record UpdateUserCommand(Set<Role> roles, Boolean active) {
    }

    public record AccountCredentials(User user, String temporaryPassword) {
    }

    public Page<User> search(String keyword, Role role, Boolean active, PageRequest page) {
        return userDao.search(keyword, role, active, page);
    }

    public User get(long userId) {
        return userDao.findById(userId).orElseThrow(() -> NotFoundException.of("tài khoản", userId));
    }

    public List<Map<String, Object>> employeesWithoutAccount() {
        return userDao.findEmployeesWithoutAccount();
    }

    public AccountCredentials create(CreateUserCommand cmd) {
        String username = Validator.lower(cmd.username());
        Set<Role> roles = cmd.roles() == null ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(nonEmpty(cmd.roles()));
        new Validator()
                .required("username", username, "Vui lòng nhập tên đăng nhập.")
                .pattern("username", username, Validator.USERNAME, "Tên đăng nhập 2–50 ký tự: chữ, số, dấu chấm, gạch dưới, gạch ngang.")
                .check(!roles.isEmpty(), "roles", "Chọn ít nhất một vai trò.")
                .check(cmd.employeeId() == null || !userDao.existsByEmployeeId(cmd.employeeId()), "employeeId",
                        "Nhân viên này đã có tài khoản.")
                .throwIfInvalid();
        String temporary = policy.generateTemporary();
        return tx.inTransaction(() -> {
            long id = userDao.create(username, hasher.hash(temporary), cmd.employeeId(), true);
            userDao.replaceRoles(id, roles);
            audit.record("USER_CREATED", "User", id, "username=" + username + ", roles=" + roles);
            return new AccountCredentials(get(id), temporary);
        });
    }

    public User update(AuthUser actor, long userId, UpdateUserCommand cmd) {
        User user = get(userId);
        Set<Role> roles = cmd.roles() == null ? user.getRoles() : EnumSet.copyOf(nonEmpty(cmd.roles()));
        boolean active = cmd.active() == null ? user.isActive() : cmd.active();
        new Validator().check(!roles.isEmpty(), "roles", "Chọn ít nhất một vai trò.").throwIfInvalid();
        if (actor.getUserId() == userId && (!active || !roles.contains(Role.ADMIN))) {
            throw new BusinessException("Không thể tự vô hiệu hóa hoặc gỡ quyền quản trị của chính mình.");
        }
        tx.inTransaction(() -> {
            userDao.replaceRoles(userId, roles);
            if (active != user.isActive()) {
                userDao.setActive(userId, active);
            }
        });
        audit.record("USER_UPDATED", "User", userId, "roles=" + roles + ", active=" + active);
        return get(userId);
    }

    public AccountCredentials resetPassword(long userId) {
        User user = get(userId);
        String temporary = policy.generateTemporary();
        userDao.updatePassword(userId, hasher.hash(temporary), true);
        audit.record("PASSWORD_RESET", "User", userId, "username=" + user.getUsername());
        return new AccountCredentials(get(userId), temporary);
    }

    public User unlock(long userId) {
        get(userId);
        userDao.unlock(userId);
        audit.record("USER_UNLOCKED", "User", userId, null);
        return get(userId);
    }

    private static Set<Role> nonEmpty(Set<Role> roles) {
        return roles.isEmpty() ? EnumSet.noneOf(Role.class) : roles;
    }
}
