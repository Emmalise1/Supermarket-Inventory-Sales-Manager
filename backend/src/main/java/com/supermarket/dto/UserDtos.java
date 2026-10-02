package com.supermarket.dto;

import com.supermarket.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** DTOs for users (passwords are write-only and never leave the server). */
public final class UserDtos {

    private UserDtos() {
    }

    public record UserDto(
            Long id,
            String email,
            String fullName,
            String role,
            Long branchId,
            boolean active,
            Instant createdAt) {

        public static UserDto from(User user) {
            return new UserDto(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole() == null ? null : user.getRole().name(),
                    user.getBranchId(),
                    user.isActive(),
                    user.getCreatedAt());
        }
    }

    public record UserRequest(
            @NotBlank @Email String email,
            @Size(min = 6, max = 72, message = "Password must be 6-72 characters") String password,
            @NotBlank String fullName,
            @NotBlank String role,
            Long branchId,
            Boolean active) {
    }
}
