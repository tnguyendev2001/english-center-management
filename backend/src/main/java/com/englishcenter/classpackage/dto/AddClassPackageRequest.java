package com.englishcenter.classpackage.dto;

import jakarta.validation.constraints.NotNull;

public record AddClassPackageRequest(
        @NotNull(message = "Tuition package id is required")
        Long tuitionPackageId
) {
}
