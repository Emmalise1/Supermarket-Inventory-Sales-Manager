package com.supermarket.security;

import com.supermarket.config.JwtConfig;
import com.supermarket.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import javax.crypto.SecretKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/** Issues an OAuth2 JWT and decodes it again - claim round-trip. */
class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private JwtService jwtService;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        SecretKey key = config.jwtSecretKey(SECRET);
        JwtEncoder encoder = config.jwtEncoder(key);
        jwtDecoder = config.jwtDecoder(key);
        jwtService = new JwtService(encoder, 480);
    }

    @Test
    void issuedToken_carriesIdentity_andRbacClaims() {
        String token = jwtService.issueToken(5L, "cashier@supermarket.rw", "POS Cashier",
                Role.CASHIER, 3L);

        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("cashier@supermarket.rw");
        assertThat(jwt.<Long>getClaim("uid")).isEqualTo(5L);
        assertThat(jwt.getClaimAsString("role")).isEqualTo("CASHIER");
        assertThat(jwt.<Long>getClaim("branch")).isEqualTo(3L);
        assertThat(jwt.getClaimAsString("name")).isEqualTo("POS Cashier");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    void tokenSignedWithDifferentSecret_isRejected() {
        String token = jwtService.issueToken(1L, "a@b.rw", "A", Role.ADMIN, null);

        JwtConfig other = new JwtConfig();
        JwtDecoder foreign = other.jwtDecoder(
                other.jwtSecretKey(Base64.getEncoder().encodeToString("another-secret-32-bytes-long!!!!".getBytes())));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> foreign.decode(token))
                .isInstanceOf(Exception.class);
    }
}
