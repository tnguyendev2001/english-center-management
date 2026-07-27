package com.englishcenter.academic.assignment.dto;

import com.englishcenter.academic.assignment.AssignmentStatus;
import com.englishcenter.academic.assignment.AssignmentTargetMode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AssignmentResponse(
        Long id,
        String title,
        String description,
        String instructions,
        Long classroomId,
        Long lessonRecordId,
        LocalDate assignedDate,
        LocalDate dueDate,
        BigDecimal maxScore,
        Boolean allowSubmission,
        Boolean allowLateSubmission,
        AssignmentTargetMode targetMode,
        List<Long> targetStudentIds,
        AssignmentStatus status,
        Long createdByTeacherId,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
