package com.hrm.service;

import com.hrm.dao.UserDao;
import com.hrm.exception.BusinessException;
import com.hrm.exception.NotFoundException;
import com.hrm.exception.ValidationException;
import com.hrm.model.User;
import com.hrm.security.PasswordHasher;
import com.hrm.security.PasswordPolicy;
import com.hrm.security.Role;
import com.hrm.service.UserService.AccountCredentials;
import com.hrm.service.UserService.CreateUserCommand;
import com.hrm.service.UserService.UpdateUserCommand;
import com.hrm.testsupport.DirectTransactions;
import com.hrm.testsupport.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao userDao;
    @Mock
    private PasswordHasher hasher;
    @Mock
    private AuditService audit;

    private final PasswordPolicy policy = new PasswordPolicy(8);
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userDao, hasher, policy, audit, new DirectTransactions());
    }

    private static User account(long id, String username, Set<Role> roles, boolean active) {
        User u = new User();
        u.setUserId(id);
        u.setUsername(username);
        u.setRoles(roles);
        u.setActive(active);
        return u;
    }

    @Test
    void createsAnAccountWithATemporaryPasswordThatSatisfiesThePolicy() {
        when(hasher.hash(anyString())).thenAnswer(inv -> "hash:" + inv.getArgument(0));
        when(userDao.create(eq("nam.lh"), anyString(), eq(13L), eq(true))).thenReturn(40L);
        when(userDao.findById(40)).thenReturn(Optional.of(account(40, "nam.lh", EnumSet.of(Role.MANAGER), true)));

        AccountCredentials created = service.create(new CreateUserCommand(" Nam.LH ", 13L, EnumSet.of(Role.MANAGER)));

        assertThat(created.temporaryPassword()).hasSize(12);
        assertThat(policy.check(created.temporaryPassword(), "nam.lh")).isNull();
        verify(userDao).create("nam.lh", "hash:" + created.temporaryPassword(), 13L, true);
        verify(userDao).replaceRoles(40, EnumSet.of(Role.MANAGER));
    }

    @Test
    void anEmployeeHasAtMostOneAccountAndEveryAccountHasARole() {
        when(userDao.existsByEmployeeId(13)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateUserCommand("x", 13L, Set.of())))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getFieldErrors())
                        .containsKeys("username", "roles", "employeeId"));
        verify(userDao, never()).create(anyString(), anyString(), anyLong(), anyBoolean());
    }

    @Test
    void anAdminCannotDisableOrDemoteHimself() {
        when(userDao.findById(1)).thenReturn(Optional.of(account(1, "admin", EnumSet.of(Role.ADMIN), true)));

        assertThatThrownBy(() -> service.update(Fixtures.admin(), 1, new UpdateUserCommand(null, false)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.update(Fixtures.admin(), 1, new UpdateUserCommand(EnumSet.of(Role.HR), null)))
                .isInstanceOf(BusinessException.class);
        verify(userDao, never()).setActive(anyLong(), anyBoolean());
    }

    @Test
    void updateChangesRolesAndOnlyTouchesStatusWhenItChanged() {
        when(userDao.findById(9)).thenReturn(Optional.of(account(9, "emp0009", EnumSet.of(Role.EMPLOYEE), true)));

        service.update(Fixtures.admin(), 9, new UpdateUserCommand(EnumSet.of(Role.EMPLOYEE, Role.MANAGER), true));
        service.update(Fixtures.admin(), 9, new UpdateUserCommand(null, false));

        verify(userDao).replaceRoles(9, EnumSet.of(Role.EMPLOYEE, Role.MANAGER));
        verify(userDao).setActive(9, false);
    }

    @Test
    void resetForcesAPasswordChangeAtNextLogin() {
        when(userDao.findById(9)).thenReturn(Optional.of(account(9, "emp0009", EnumSet.of(Role.EMPLOYEE), true)));
        when(hasher.hash(anyString())).thenReturn("hash");

        AccountCredentials reset = service.resetPassword(9);

        assertThat(reset.temporaryPassword()).isNotBlank();
        verify(userDao).updatePassword(9, "hash", true);
        verify(audit).record("PASSWORD_RESET", "User", 9L, "username=emp0009");
    }

    @Test
    void unlockingAnUnknownAccountIsNotFound() {
        when(userDao.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unlock(404)).isInstanceOf(NotFoundException.class);
        verify(userDao, never()).unlock(anyLong());
    }

    @Test
    void temporaryPasswordsAreRandom() {
        assertThat(policy.generateTemporary()).isNotEqualTo(policy.generateTemporary());
    }
}
