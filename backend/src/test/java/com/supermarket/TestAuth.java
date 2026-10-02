package com.supermarket;

import com.supermarket.domain.Role;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;

/**
 * Test helper: places a JWT authentication into the SecurityContextHolder
 * so that {@link com.supermarket.security.AuthContext} behaves exactly as
 * it does at runtime (instead of being mocked away).
 */
public final class TestAuth {

    private TestAuth() {
    }

    public static void authenticate(Long uid, String email, Role role, Long branchId) {
        Jwt jwt = buildToken(uid, email, role, branchId);
        SecurityContextHolderSetup.set(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    public static Jwt buildToken(Long uid, String email, Role role, Long branchId) {
        Instant now = Instant.now();
        var builder = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(email)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("uid", uid)
                .claim("name", "Test User")
                .claim("role", role.name());
        if (branchId != null) {
            builder.claim("branch", branchId);
        }
        return builder.build();
    }

    /** Thin indirection so imports stay in one place. */
    static final class SecurityContextHolderSetup {
        private SecurityContextHolderSetup() {
        }

        static void set(JwtAuthenticationToken token) {
            org.springframework.security.core.context.SecurityContextHolder
                    .getContext().setAuthentication(token);
        }
    }

    public static void clear() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}
