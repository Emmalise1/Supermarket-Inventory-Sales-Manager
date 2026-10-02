package com.supermarket.service;

import com.supermarket.domain.User;
import com.supermarket.dto.AuthDtos.LoginRequest;
import com.supermarket.dto.AuthDtos.TokenResponse;
import com.supermarket.dto.UserDtos.UserDto;
import com.supermarket.repository.UserRepository;
import com.supermarket.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Email/password authentication issuing OAuth2 JWT access tokens. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final long ttlMinutes;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditService auditService,
                       @Value("${app.security.jwt.ttl-minutes:480}") long ttlMinutes) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.ttlMinutes = ttlMinutes;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new BadCredentialsException("Account is disabled");
        }

        String token = jwtService.issueToken(
                user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getBranchId());

        auditService.record("USER_LOGIN", "User", user.getId(), "User logged in: " + user.getEmail());

        return new TokenResponse(token, "Bearer", ttlMinutes * 60, UserDto.from(user));
    }
}
