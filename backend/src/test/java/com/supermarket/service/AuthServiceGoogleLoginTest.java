package com.supermarket.service;

import com.supermarket.domain.Role;
import com.supermarket.domain.User;
import com.supermarket.dto.AuthDtos.TokenResponse;
import com.supermarket.repository.UserRepository;
import com.supermarket.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OAuth2 (Google) sign-in must reuse the account that an admin already provisioned:
 * it never creates users (RBAC stays under admin control), never touches the password
 * hash, and audits the login.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceGoogleLoginTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditService auditService;

    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, auditService, 480);
        user = new User("john.doe@gmail.com", "{bcrypt}secret", "John Doe", Role.CASHIER, 2L);
        user.setId(9L);
    }

    @Test
    void unknownGoogleAccount_isRejectedAndNeverProvisioned() {
        when(userRepository.findByEmail("stranger@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginWithGoogle("Stranger@Gmail.com"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("No account is registered");

        verify(userRepository, never()).save(any());
        verify(jwtService, never()).issueToken(any(), any(), any(), any(), any());
    }

    @Test
    void disabledAccount_isRejected() {
        user.setActive(false);
        when(userRepository.findByEmail("john.doe@gmail.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.loginWithGoogle("john.doe@gmail.com"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("disabled");

        verify(jwtService, never()).issueToken(any(), any(), any(), any(), any());
    }

    @Test
    void knownAccount_issuesJwtWithItsExistingRoleAndAuditsTheLogin() {
        when(userRepository.findByEmail("john.doe@gmail.com")).thenReturn(Optional.of(user));
        when(jwtService.issueToken(9L, "john.doe@gmail.com", "John Doe", Role.CASHIER, 2L))
                .thenReturn("google-jwt");

        TokenResponse response = authService.loginWithGoogle("  JOHN.DOE@GMAIL.COM ");

        assertThat(response.token()).isEqualTo("google-jwt");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("john.doe@gmail.com");
        assertThat(response.user().role()).isEqualTo("CASHIER");
        verify(auditService).record("USER_LOGIN_GOOGLE", "User", 9L,
                "Signed in with Google: john.doe@gmail.com");
        // the password hash is irrelevant for an OAuth2 sign-in
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}
