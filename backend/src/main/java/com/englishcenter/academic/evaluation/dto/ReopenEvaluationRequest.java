package com.englishcenter.academic.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReopenEvaluationRequest(
        @NotBlank @Size(max = 1000) String reason
) {
}
