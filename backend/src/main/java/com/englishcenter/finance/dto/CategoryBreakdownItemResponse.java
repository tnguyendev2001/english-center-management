package com.englishcenter.finance.dto;

import com.englishcenter.finance.CategoryDirection;
import java.math.BigDecimal;

public record CategoryBreakdownItemResponse(
        Long categoryId,
        String categoryCode,
        String categoryName,
        CategoryDirection direction,
        BigDecimal amount
) {
}
