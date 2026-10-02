package com.supermarket.security;

import com.supermarket.domain.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Issues OAuth2-style JWT access tokens signed with HS256.
 * Tokens carry the user id, RBAC role and branch for stateless
 * authentication and authorization on every request.
 */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long ttlMinutes;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${app.security.jwt.ttl-minutes:480}") long ttlMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.ttlMinutes = ttlMinutes;
    }

    public String issueToken(Long userId, String email, String fullName, Role role, Long branchId) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("supermarket-backend")
                .subject(email)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlMinutes * 60))
                .claim("uid", userId)
                .claim("name", fullName)
                .claim("role", role.name())
                .claim("jti", UUID.randomUUID().toString());
        if (branchId != null) {
            claims.claim("branch", branchId);
        }
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
    }
}
