package com.englishcenter.finance.dto;

import java.util.List;

public record TransferResponse(
        String transferGroupId,
        CashTransactionResponse outflow,
        CashTransactionResponse inflow,
        List<String> warnings
) {
}
