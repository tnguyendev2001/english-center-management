package com.englishcenter.tuitionpackage.dto;

import com.englishcenter.tuitionpackage.TuitionPackageStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TuitionPackageResponse(
        Long id,
        String name,
        Integer sessionsPerWeek,
        Integer totalSessions,
        Integer expectedMonths,
        BigDecimal price,
        TuitionPackageStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
