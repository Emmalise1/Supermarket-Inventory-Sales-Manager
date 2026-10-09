package com.supermarket.security;

import com.supermarket.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The OAuth2 callback never renders a page: it always redirects back to the SPA with
 * either the JWT or an explanation, and the JWT travels in the URL fragment so it is
 * never sent to (or logged by) a server.
 */
@ExtendWith(MockitoExtension.class)
class GoogleOAuth2HandlersTest {

    private static final String REDIRECT_BASE = "http://localhost:5173/login";

    @Mock
    private AuthService authService;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Authentication authentication;
    @Mock
    private OAuth2User googleUser;

    private GoogleOAuth2Handlers handlers;

    @BeforeEach
    void setUp() {
        handlers = new GoogleOAuth2Handlers(authService, REDIRECT_BASE);
        lenient().when(authentication.getPrincipal()).thenReturn(googleUser);
        lenient().when(googleUser.getAttribute("email")).thenReturn("john.doe@gmail.com");
    }

    private String redirectLocation() throws Exception {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(captor.capture());
        return captor.getValue();
    }

    @Test
    void knownAccount_redirectsWithTheTokenInTheFragment() throws Exception {
        when(authService.loginWithGoogle("john.doe@gmail.com"))
                .thenReturn(new com.supermarket.dto.AuthDtos.TokenResponse(
                        "jwt-value", "Bearer", 3600, null));

        handlers.onAuthenticationSuccess(null, response, authentication);

        String location = redirectLocation();
        assertThat(location).startsWith(REDIRECT_BASE + "#token=");
        // fragment, not query string: never reaches the server or its logs
        assertThat(location).doesNotContain("?token=");
        assertThat(location).contains("jwt-value");
    }

    @Test
    void unknownAccount_redirectsWithAnExplanationInsteadOfAToken() throws Exception {
        when(authService.loginWithGoogle("john.doe@gmail.com"))
                .thenThrow(new BadCredentialsException("No account is registered for john.doe@gmail.com"));

        handlers.onAuthenticationSuccess(null, response, authentication);

        String location = redirectLocation();
        assertThat(location).startsWith(REDIRECT_BASE + "#error=");
        assertThat(location).doesNotContain("#token=");
        assertThat(location).contains("No+account+is+registered");
    }

    @Test
    void googleWithoutAnEmail_redirectsWithAnError() throws Exception {
        when(googleUser.getAttribute("email")).thenReturn(null);

        handlers.onAuthenticationSuccess(null, response, authentication);

        assertThat(redirectLocation()).startsWith(REDIRECT_BASE + "#error=");
        verify(authService, org.mockito.Mockito.never()).loginWithGoogle(anyString());
    }

    @Test
    void failedFlow_redirectsWithTheProviderMessage() throws Exception {
        handlers.onAuthenticationFailure(null, response,
                new org.springframework.security.oauth2.core.OAuth2AuthenticationException(
                        new org.springframework.security.oauth2.core.OAuth2Error("access_denied",
                                "The user denied the request", null)));

        assertThat(redirectLocation()).startsWith(REDIRECT_BASE + "#error=");
    }
}
