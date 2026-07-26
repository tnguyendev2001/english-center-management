package com.englishcenter.classroom.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ClassroomRenewalItemRequest(
        @NotNull Long enrollmentId,
        @NotNull Long tuitionPackageId,
        LocalDate effectiveDate
) {
}
