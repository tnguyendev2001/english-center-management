package com.englishcenter.academic.material.dto;

import com.englishcenter.academic.material.MaterialVisibility;
import java.time.LocalDateTime;

public record LearningMaterialResponse(
        Long id,
        String title,
        String description,
        String fileName,
        String contentType,
        Long fileSize,
        Long classroomId,
        Long lessonRecordId,
        Long assignmentId,
        Long assessmentId,
        MaterialVisibility visibility,
        Boolean active,
        String uploadedBy,
        LocalDateTime uploadedAt
) {
}
