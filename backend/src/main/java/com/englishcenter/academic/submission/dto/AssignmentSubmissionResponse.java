package com.englishcenter.academic.submission.dto;

import com.englishcenter.academic.submission.SubmissionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AssignmentSubmissionResponse(
        Long id,
        Long assignmentId,
        Long studentId,
        LocalDateTime submittedAt,
        String textAnswer,
        SubmissionStatus status,
        BigDecimal teacherScore,
        String teacherFeedback,
        LocalDateTime gradedAt,
        String gradedBy,
        List<AssignmentSubmissionAttachmentResponse> attachments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
