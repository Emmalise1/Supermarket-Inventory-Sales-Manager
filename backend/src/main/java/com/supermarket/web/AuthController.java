package com.supermarket.web;

import com.supermarket.dto.AuthDtos.LoginRequest;
import com.supermarket.dto.AuthDtos.TokenResponse;
import com.supermarket.dto.UserDtos.UserDto;
import com.supermarket.repository.UserRepository;
import com.supermarket.security.AuthContext;
import com.supermarket.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authentication endpoints. {@code POST /api/auth/login} is public. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthContext authContext;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, AuthContext authContext, UserRepository userRepository) {
        this.authService = authService;
        this.authContext = authContext;
        this.userRepository = userRepository;
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
