package com.englishcenter.packagechange.dto;

import com.englishcenter.packagechange.PackageChangeMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChangePackageRequest(
        @NotNull
        Long newTuitionPackageId,

        @NotNull
        PackageChangeMode changeMode,

        @NotBlank
        String reason
) {
}
