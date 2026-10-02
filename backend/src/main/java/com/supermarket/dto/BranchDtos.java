package com.supermarket.dto;

import com.supermarket.domain.Branch;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/** DTOs for branches. */
public final class BranchDtos {

    private BranchDtos() {
    }

    public record BranchDto(
            Long id,
            String name,
            String address,
            String phone,
            boolean active,
            Instant createdAt) {

        public static BranchDto from(Branch branch) {
            return new BranchDto(branch.getId(), branch.getName(), branch.getAddress(),
                    branch.getPhone(), branch.isActive(), branch.getCreatedAt());
        }
    }

    public record BranchRequest(
            @NotBlank String name,
            String address,
            String phone,
            Boolean active) {
    }
}
