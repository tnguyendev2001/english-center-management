package com.englishcenter.classpackage.dto;

import com.englishcenter.tuitionpackage.TuitionPackageStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClassPackageResponse(
        Long id,
        Long classroomId,
        Long tuitionPackageId,
        String packageName,
        Integer sessionsPerWeek,
        Integer totalSessions,
        Integer expectedMonths,
        BigDecimal price,
        TuitionPackageStatus tuitionPackageStatus,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
