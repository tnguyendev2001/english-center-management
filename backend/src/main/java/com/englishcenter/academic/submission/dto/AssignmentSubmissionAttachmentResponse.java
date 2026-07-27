package com.englishcenter.academic.submission.dto;

import java.time.LocalDateTime;

public record AssignmentSubmissionAttachmentResponse(
        Long id,
        String fileName,
        String contentType,
        Long fileSize,
        LocalDateTime uploadedAt
) {
}
