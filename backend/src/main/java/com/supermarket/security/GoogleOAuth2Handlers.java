package com.supermarket.security;

import com.supermarket.dto.AuthDtos.TokenResponse;
import com.supermarket.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Bridges Google OAuth2 sign-in to the JWT the rest of the application already uses.
 *
 * <ul>
 *   <li>Success: the Google email must match an existing, active account (accounts are never
 *       auto-provisioned from an external IdP, so RBAC stays under admin control), then a normal
 *       HS256 JWT is issued and the browser is sent back to the SPA.</li>
 *   <li>The JWT travels in the URL <em>fragment</em> ({@code #token=...}), which browsers never
 *       send to a server - it is not written to access logs or proxy logs.</li>
 *   <li>Failure: redirected to the SPA with {@code #error=...} so the login page can show why.</li>
 * </ul>
 */
@Component
public class GoogleOAuth2Handlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuth2Handlers.class);

    private final AuthService authService;
    private final String postLoginRedirect;

    public GoogleOAuth2Handlers(AuthService authService,
                                @Value("${app.security.google.post-login-redirect:http://localhost:5173/login}")
                                String postLoginRedirect) {
        this.authService = authService;
        this.postLoginRedirect = postLoginRedirect;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String email = emailOf(authentication);
        if (email == null || email.isBlank()) {
            redirectWithError(response, "Google did not return an email address for this account.");
            return;
        }
        try {
            TokenResponse token = authService.loginWithGoogle(email);
            response.sendRedirect(postLoginRedirect + "#token=" + encode(token.token()));
        } catch (Exception ex) {
            log.info("Google sign-in rejected for {}: {}", email, ex.getMessage());
            redirectWithError(response, ex.getMessage());
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException ex) throws IOException {
        String message = (ex == null || ex.getMessage() == null)
                ? "Google sign-in was cancelled or refused."
                : ex.getMessage();
        log.info("Google sign-in failed: {}", message);
        redirectWithError(response, message);
    }

    private void redirectWithError(HttpServletResponse response, String message) throws IOException {
        response.sendRedirect(postLoginRedirect + "#error=" + encode(message));
    }

    /** Prefers the OIDC claim, falls back to the plain OAuth2 user-info attribute. */
    private String emailOf(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getEmail();
        }
        if (principal instanceof OAuth2User oauth2User) {
            Object email = oauth2User.getAttribute("email");
            return email == null ? null : email.toString();
        }
        return null;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
