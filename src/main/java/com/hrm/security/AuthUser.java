package com.hrm.security;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class AuthUser implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String API_CLIENT = "api-client";

    private final long userId;
    private final String username;
    private final Long employeeId;
    private final String fullName;
    private final Set<Role> roles;

    public AuthUser(long userId, String username, Long employeeId, String fullName, Set<Role> roles) {
        this.userId = userId;
        this.username = username;
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.roles = roles.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(roles));
    }

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public boolean hasRole(Role role) {
        return roles.contains(Role.ADMIN) || roles.contains(role);
    }

    public boolean hasAnyRole(Set<Role> required) {
        return required.isEmpty() || required.stream().anyMatch(this::hasRole);
    }

    public boolean isHrOrAdmin() {
        return hasRole(Role.HR);
    }

    public boolean isAdmin() {
        return roles.contains(Role.ADMIN);
    }

    public boolean isHr() {
        return hasRole(Role.HR);
    }

    public boolean isManager() {
        return hasRole(Role.MANAGER);
    }

    public boolean isHasEmployee() {
        return employeeId != null;
    }

    @Override
    public String toString() {
        return username + roles;
    }
}
