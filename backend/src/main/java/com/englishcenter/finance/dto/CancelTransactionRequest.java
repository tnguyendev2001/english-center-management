package com.englishcenter.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelTransactionRequest(
        @NotBlank(message = "Reason is required")
        @Size(max = 1000)
        String reason,

        @Size(max = 100)
        String canceledBy
) {
}
