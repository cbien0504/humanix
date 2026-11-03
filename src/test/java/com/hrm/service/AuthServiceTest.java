package com.hrm.service;

import com.hrm.dao.UserDao;
import com.hrm.exception.UnauthorizedException;
import com.hrm.exception.ValidationException;
import com.hrm.model.User;
import com.hrm.security.AuthUser;
import com.hrm.security.PasswordHasher;
import com.hrm.security.PasswordPolicy;
import com.hrm.security.Role;
import com.hrm.testsupport.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String DUMMY_HASH = "$2a$10$dummy";
    private static final String HASH = "$2a$10$stored";

    @Mock
    private UserDao userDao;
    @Mock
    private PasswordHasher hasher;
    @Mock
    private AuditService audit;

    private final Clock clock = Fixtures.clock();
    private final Instant now = clock.instant();
    private AuthService service;

    @BeforeEach
    void setUp() {
        when(hasher.hash(anyString())).thenReturn(DUMMY_HASH);
        service = new AuthService(userDao, hasher, new PasswordPolicy(8), audit, clock, 5, 15);
    }

    private static AuthUser principal() {
        return new AuthUser(7, "hr", 5L, "Lê Thị Lan", EnumSet.of(Role.HR, Role.EMPLOYEE));
    }

    private static User user(boolean active) {
        User u = new User();
        u.setUserId(7);
        u.setUsername("hr");
        u.setPasswordHash(HASH);
        u.setEmployeeId(5L);
        u.setFullName("Lê Thị Lan");
        u.setActive(active);
        u.setRoles(EnumSet.of(Role.HR, Role.EMPLOYEE));
        return u;
    }

    @Nested
    class Login {

        @Test
        void successReturnsThePrincipalAndRecordsTheLogin() {
            User u = user(true);
            u.setMustChangePassword(true);
            when(userDao.findByUsername("hr")).thenReturn(Optional.of(u));
            when(hasher.matches("Demo@123", HASH)).thenReturn(true);

            AuthService.LoginResult r = service.login("  hr ", "Demo@123");

            assertThat(r.user().getUsername()).isEqualTo("hr");
            assertThat(r.user().getEmployeeId()).isEqualTo(5L);
            assertThat(r.user().isHr()).isTrue();
            assertThat(r.mustChangePassword()).isTrue();
            verify(userDao).recordLoginSuccess(7, now);
        }

        @Test
        void unknownUserSpendsABcryptCheckAndGetsTheSameMessage() {
            when(userDao.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.login("ghost", "whatever1A"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage(AuthService.BAD_CREDENTIALS);
            verify(hasher).matches("whatever1A", DUMMY_HASH);
        }

        @Test
        void wrongPasswordCountsAFailure() {
            when(userDao.findByUsername("hr")).thenReturn(Optional.of(user(true)));
            when(hasher.matches("wrong", HASH)).thenReturn(false);
            when(userDao.recordLoginFailure(7, 5, now.plus(Duration.ofMinutes(15)))).thenReturn(2);

            assertThatThrownBy(() -> service.login("hr", "wrong"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage(AuthService.BAD_CREDENTIALS);
            verify(audit).record("hr", "LOGIN_FAILED", "User", 7L, "Sai mật khẩu lần 2");
        }

        @Test
        void fifthFailureLocksTheAccountForFifteenMinutes() {
            when(userDao.findByUsername("hr")).thenReturn(Optional.of(user(true)));
            when(hasher.matches("wrong", HASH)).thenReturn(false);
            when(userDao.recordLoginFailure(eq(7L), eq(5), any())).thenReturn(5);

            assertThatThrownBy(() -> service.login("hr", "wrong"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("tạm khóa")
                    .hasMessageContaining("09:15");
            verify(audit).record("hr", "ACCOUNT_LOCKED", "User", 7L, "Khóa 15 phút");
        }

        @Test
        void whileLockedThePasswordIsNotEvenChecked() {
            User locked = user(true);
            locked.setLockedUntil(now.plus(Duration.ofMinutes(10)));
            when(userDao.findByUsername("hr")).thenReturn(Optional.of(locked));

            assertThatThrownBy(() -> service.login("hr", "Demo@123"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("09:10");
            verify(hasher, never()).matches(anyString(), anyString());
            verify(userDao, never()).recordLoginFailure(anyLong(), anyInt(), any());
        }

        @Test
        void disabledAccountIsOnlyRevealedToSomeoneWhoKnowsThePassword() {
            when(userDao.findByUsername("hr")).thenReturn(Optional.of(user(false)));
            when(hasher.matches("Demo@123", HASH)).thenReturn(true);
            when(hasher.matches("wrong", HASH)).thenReturn(false);
            when(userDao.recordLoginFailure(anyLong(), anyInt(), any())).thenReturn(1);

            assertThatThrownBy(() -> service.login("hr", "wrong")).hasMessage(AuthService.BAD_CREDENTIALS);
            assertThatThrownBy(() -> service.login("hr", "Demo@123")).hasMessageContaining("vô hiệu hóa");
            verify(userDao, never()).recordLoginSuccess(anyLong(), any());
        }

        @Test
        void blankCredentialsAreRejectedBeforeTheDatabase() {
            assertThatThrownBy(() -> service.login("  ", "x")).isInstanceOf(UnauthorizedException.class);
            assertThatThrownBy(() -> service.login("hr", "")).isInstanceOf(UnauthorizedException.class);
            verify(userDao, never()).findByUsername(anyString());
        }
    }

    @Nested
    class ChangePassword {

        @BeforeEach
        void currentUser() {
            when(userDao.findById(7)).thenReturn(Optional.of(user(true)));
        }

        private ValidationException change(String current, String next, String confirm) {
            try {
                service.changePassword(principal(), current, next, confirm);
            } catch (ValidationException e) {
                verify(userDao, never()).updatePassword(anyLong(), anyString(), anyBoolean());
                return e;
            }
            throw new AssertionError("expected a ValidationException");
        }

        @Test
        void currentPasswordMustBeRight() {
            when(hasher.matches("bad", HASH)).thenReturn(false);

            assertThat(change("bad", "NewPass123", "NewPass123").getFieldErrors()).containsKey("currentPassword");
        }

        @Test
        void newPasswordMustFollowThePolicyDifferAndBeConfirmed() {
            when(hasher.matches("Demo@123", HASH)).thenReturn(true);

            assertThat(change("Demo@123", "short", "short").getFieldErrors().get("newPassword")).contains("ít nhất 8");
            assertThat(change("Demo@123", "alllowercase1", "alllowercase1").getFieldErrors()).containsKey("newPassword");
            assertThat(change("Demo@123", "Demo@123", "Demo@123").getFieldErrors().get("newPassword")).contains("khác");
            assertThat(change("Demo@123", "NewPass123", "NewPass124").getFieldErrors()).containsKey("confirmPassword");
        }

        @Test
        void storesTheNewHashAndClearsTheMustChangeFlag() {
            when(hasher.matches("Demo@123", HASH)).thenReturn(true);
            when(hasher.hash("NewPass123")).thenReturn("$2a$10$new");

            service.changePassword(principal(), "Demo@123", "NewPass123", "NewPass123");

            verify(userDao).updatePassword(7, "$2a$10$new", false);
            verify(audit).record("hr", "PASSWORD_CHANGED", "User", 7L, null);
        }
    }
}
