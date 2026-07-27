package com.englishcenter.academic.submission.dto;

import com.englishcenter.academic.submission.SubmissionStatus;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record GradeSubmissionRequest(
        BigDecimal teacherScore,
        String teacherFeedback,
        @NotNull SubmissionStatus status
) {
}
