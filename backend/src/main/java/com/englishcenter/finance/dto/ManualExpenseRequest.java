package com.englishcenter.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ManualExpenseRequest(
        @NotNull(message = "Transaction date is required")
        LocalDate transactionDate,

        @NotNull(message = "Account is required")
        Long accountId,

        @NotNull(message = "Category is required")
        Long categoryId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @Size(max = 200)
        String payerOrPayee,

        @Size(max = 100)
        String referenceNo,

        @NotBlank(message = "Description is required")
        @Size(max = 1000)
        String description,

        @Size(max = 500)
        String attachmentReference
) {
}
