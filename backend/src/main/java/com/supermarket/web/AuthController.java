package com.supermarket.web;

import com.supermarket.dto.AuthDtos.LoginRequest;
import com.supermarket.dto.AuthDtos.TokenResponse;
import com.supermarket.dto.UserDtos.UserDto;
import com.supermarket.repository.UserRepository;
import com.supermarket.security.AuthContext;
import com.supermarket.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Authentication endpoints. {@code POST /api/auth/login} and {@code GET /api/auth/providers} are public. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthContext authContext;
    private final UserRepository userRepository;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;

    public AuthController(AuthService authService, AuthContext authContext, UserRepository userRepository,
                          ObjectProvider<ClientRegistrationRepository> clientRegistrations) {
        this.authService = authService;
        this.authContext = authContext;
        this.userRepository = userRepository;
        this.clientRegistrations = clientRegistrations;
    }

    /**
     * Public: tells the login page which sign-in methods are configured, so the
     * "Continue with Google" button only appears when OAuth2 credentials are set.
     */
    @GetMapping("/providers")
    public Map<String, Boolean> providers() {
        boolean googleEnabled = clientRegistrations.getIfAvailable() != null;
        return Map.of("password", true, "google", googleEnabled);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Returns the profile encoded in the current JWT. */
    @GetMapping("/me")
    public UserDto me() {
        AuthContext.AuthUser user = authContext.require();
        return userRepository.findById(user.id())
                .map(UserDto::from)
                .orElseGet(() -> new UserDto(user.id(), user.email(), user.name(),
                        user.role().name(), user.branchId(), true, null));
    }
}
