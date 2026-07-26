package com.englishcenter.finance.dto;

import com.englishcenter.finance.CategoryDirection;
import java.time.LocalDateTime;

public record TransactionCategoryResponse(
        Long id,
        String code,
        String name,
        CategoryDirection direction,
        Long parentId,
        boolean active,
        boolean systemCategory,
        int displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
