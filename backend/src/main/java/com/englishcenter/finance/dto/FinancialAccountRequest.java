package com.englishcenter.finance.dto;

import com.englishcenter.finance.FinancialAccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialAccountRequest(
        @NotBlank(message = "Code is required")
        @Size(max = 50)
        String code,

        @NotBlank(message = "Name is required")
        @Size(max = 200)
        String name,

        @NotNull(message = "Type is required")
        FinancialAccountType type,

        @NotNull(message = "Opening balance is required")
        BigDecimal openingBalance,

        @NotNull(message = "Opening balance date is required")
        LocalDate openingBalanceDate,

        @Size(max = 1000)
        String note,

        Integer displayOrder
) {
}
