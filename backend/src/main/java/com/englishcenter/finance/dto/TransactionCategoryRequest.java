package com.englishcenter.finance.dto;

import com.englishcenter.finance.CategoryDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransactionCategoryRequest(
        @NotBlank(message = "Code is required")
        @Size(max = 50)
        String code,

        @NotBlank(message = "Name is required")
        @Size(max = 200)
        String name,

        @NotNull(message = "Direction is required")
        CategoryDirection direction,

        Long parentId,

        Integer displayOrder
) {
}
