package com.hrm.security;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The authenticated principal stored in the HTTP session (kept small and serialisable).
 * {@link Role#ADMIN} implicitly satisfies every role check.
 */
public final class AuthUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Synthetic principal used for system-to-system calls authenticated by API key. */
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

    /** Linked employee record, or {@code null} for technical accounts. */
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

    /** HR staff and administrators see company-wide data. */
    public boolean isHrOrAdmin() {
        return hasRole(Role.HR);
    }

    // EL-friendly accessors used by the JSP menu
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
