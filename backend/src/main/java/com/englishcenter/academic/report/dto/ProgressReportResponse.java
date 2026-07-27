package com.englishcenter.academic.report.dto;

import com.englishcenter.academic.report.ProgressReportStatus;
import java.time.LocalDateTime;

public record ProgressReportResponse(
        Long id,
        Long evaluationId,
        Long evaluationPeriodId,
        Long classroomId,
        Long studentId,
        Long teacherId,
        ProgressReportStatus status,
        LocalDateTime publishedAt,
        LocalDateTime generatedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
