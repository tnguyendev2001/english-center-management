package com.englishcenter.enrollment.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelEnrollmentRequest(@NotBlank String reason) {
}
