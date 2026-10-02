package com.supermarket.service;

import com.supermarket.domain.Role;
import com.supermarket.domain.User;
import com.supermarket.dto.UserDtos.UserDto;
import com.supermarket.dto.UserDtos.UserRequest;
import com.supermarket.exception.BusinessRuleException;
import com.supermarket.exception.DuplicateResourceException;
import com.supermarket.exception.ResourceNotFoundException;
import com.supermarket.repository.UserRepository;
import com.supermarket.security.AuthContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** User management (admin only - enforced with @PreAuthorize on the controller). */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthContext authContext;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthContext authContext,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authContext = authContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return userRepository.findAll().stream().map(UserDto::from).toList();
    }

    @Transactional
    public UserDto create(UserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email " + email + " is already registered");
        }
        if (request.password() == null || request.password().length() < 6) {
            throw new BusinessRuleException("Password must be at least 6 characters");
        }
        Role role = parseRole(request.role());
        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                role,
                request.branchId());
        user.setActive(request.active() == null || request.active());
        user = userRepository.save(user);

        auditService.record("USER_CREATED", "User", user.getId(),
                "Created user " + user.getEmail() + " with role " + role);
        return UserDto.from(user);
    }

    @Transactional
    public UserDto update(Long id, UserRequest request) {
        User user = require(id);
        String email = request.email().trim().toLowerCase();
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email " + email + " is already registered");
        }
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        if (request.role() != null && !request.role().isBlank()) {
            user.setRole(parseRole(request.role()));
        }
        if (request.branchId() != null) {
            user.setBranchId(request.branchId());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        if (request.password() != null && !request.password().isBlank()) {
            if (request.password().length() < 6) {
                throw new BusinessRuleException("Password must be at least 6 characters");
            }
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        // A user must never lock themselves out by deactivating their own account.
        AuthContext.AuthUser current = authContext.require();
        if (current.id().equals(user.getId()) && !user.isActive()) {
            throw new BusinessRuleException("You cannot deactivate your own account");
        }
        user = userRepository.save(user);
        auditService.record("USER_UPDATED", "User", user.getId(), "Updated user " + user.getEmail());
        return UserDto.from(user);
    }

    private User require(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
    }

    private static Role parseRole(String role) {
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Unknown role: " + role);
        }
    }
}
