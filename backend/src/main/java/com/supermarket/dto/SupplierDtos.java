package com.supermarket.dto;

import com.supermarket.domain.Supplier;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/** DTOs for suppliers. */
public final class SupplierDtos {

    private SupplierDtos() {
    }

    public record SupplierDto(
            Long id,
            String name,
            String contactPerson,
            String phone,
            String email,
            boolean active,
            Instant createdAt) {

        public static SupplierDto from(Supplier s) {
            return new SupplierDto(s.getId(), s.getName(), s.getContactPerson(), s.getPhone(),
                    s.getEmail(), s.isActive(), s.getCreatedAt());
        }
    }

    public record SupplierRequest(
            @NotBlank String name,
            String contactPerson,
            String phone,
            String email,
            Boolean active) {
    }
}
