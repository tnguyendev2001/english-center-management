package com.englishcenter.packagechange.dto;

import com.englishcenter.packagechange.PackageChangeMode;
import jakarta.validation.constraints.NotNull;

public record ChangePackagePreviewRequest(
        @NotNull
        Long newTuitionPackageId,

        @NotNull
        PackageChangeMode changeMode
) {
}
