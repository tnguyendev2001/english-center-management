package com.englishcenter.finance.dto;

import jakarta.validation.constraints.Size;

public record ClosePeriodRequest(
        @Size(max = 1000)
        String note,

        boolean overrideReconciliationMismatch,

        @Size(max = 100)
        String closedBy
) {
}
