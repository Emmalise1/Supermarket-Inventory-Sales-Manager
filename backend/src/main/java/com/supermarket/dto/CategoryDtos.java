package com.supermarket.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/** DTOs for product categories. */
public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CategoryDto(
            Long id,
            String name) {

        public static CategoryDto from(com.supermarket.domain.Category category) {
            return new CategoryDto(category.getId(), category.getName());
        }
    }

    public record CategoryRequest(
            @NotBlank String name) {
    }
}
