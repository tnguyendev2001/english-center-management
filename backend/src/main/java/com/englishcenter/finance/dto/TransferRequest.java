package com.englishcenter.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferRequest(
        @NotNull(message = "Transaction date is required")
        LocalDate transactionDate,

        @NotNull(message = "Source account is required")
        Long sourceAccountId,

        @NotNull(message = "Destination account is required")
        Long destinationAccountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "Description is required")
        @Size(max = 1000)
        String description,

        @Size(max = 100)
        String referenceNo
) {
}
