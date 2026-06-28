package com.englishcenter.classroom.dto;

import jakarta.validation.constraints.NotNull;

public record ClassroomRenewalItemRequest(
        @NotNull Long enrollmentId,
        @NotNull Long tuitionPackageId
) {
}
