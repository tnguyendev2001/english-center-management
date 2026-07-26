package com.englishcenter.finance.dto;

import com.englishcenter.finance.TransactionDirection;
import com.englishcenter.finance.TransactionSourceType;
import com.englishcenter.finance.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CashTransactionResponse(
        Long id,
        String transactionCode,
        LocalDate transactionDate,
        Long accountId,
        String accountCode,
        String accountName,
        TransactionDirection direction,
        Long categoryId,
        String categoryCode,
        String categoryName,
        BigDecimal amount,
        BigDecimal incomeAmount,
        BigDecimal expenseAmount,
        BigDecimal balanceAfter,
        TransactionStatus status,
        TransactionSourceType sourceType,
        Long sourceId,
        String referenceNo,
        String payerOrPayee,
        String description,
        String attachmentReference,
        String transferGroupId,
        Long originalTransactionId,
        String originalTransactionCode,
        Long reversalTransactionId,
        String reversalTransactionCode,
        String cancelReason,
        LocalDateTime postedAt,
        LocalDateTime canceledAt,
        String canceledBy,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy,
        List<String> warnings
) {
}
