package com.hrm.dao;

import com.hrm.model.Page;
import com.hrm.model.PageRequest;
import com.hrm.model.User;
import com.hrm.security.Role;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Persistence of login accounts and their roles. */
public interface UserDao {

    Optional<User> findByUsername(String username);

    Optional<User> findById(long userId);

    Page<User> search(String keyword, Role role, Boolean active, PageRequest page);

    long create(String username, String passwordHash, Long employeeId, boolean mustChangePassword);

    void replaceRoles(long userId, Set<Role> roles);

    void setActive(long userId, boolean active);

    void updatePassword(long userId, String passwordHash, boolean mustChangePassword);

    void recordLoginSuccess(long userId, Instant at);

    /**
     * Atomically increments the failure counter and locks the account when it reaches
     * {@code maxFailures}. Returns the new counter value.
     */
    int recordLoginFailure(long userId, int maxFailures, Instant lockUntil);

    void unlock(long userId);

    boolean existsByEmployeeId(long employeeId);

    /** Employees (id → "code – name") that are still working and have no account yet. */
    List<Map<String, Object>> findEmployeesWithoutAccount();
}
