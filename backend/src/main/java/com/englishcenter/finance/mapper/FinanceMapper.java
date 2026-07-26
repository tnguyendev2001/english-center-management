package com.englishcenter.finance.mapper;

import com.englishcenter.finance.CashTransaction;
import com.englishcenter.finance.FinancialAccount;
import com.englishcenter.finance.FinancialPeriod;
import com.englishcenter.finance.TransactionCategory;
import com.englishcenter.finance.TransactionDirection;
import com.englishcenter.finance.dto.CashTransactionResponse;
import com.englishcenter.finance.dto.FinancialAccountResponse;
import com.englishcenter.finance.dto.FinancialPeriodResponse;
import com.englishcenter.finance.dto.TransactionCategoryResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FinanceMapper {
    public FinancialAccountResponse toAccountResponse(
            FinancialAccount account,
            BigDecimal currentBalance,
            LocalDate lastTransactionDate
    ) {
        boolean negative = currentBalance != null && currentBalance.compareTo(BigDecimal.ZERO) < 0;
        return new FinancialAccountResponse(
                account.getId(),
                account.getCode(),
                account.getName(),
                account.getType(),
                account.getOpeningBalance(),
                account.getOpeningBalanceDate(),
                currentBalance,
                negative,
                account.isActive(),
                account.getNote(),
                account.getDisplayOrder(),
                lastTransactionDate,
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    public TransactionCategoryResponse toCategoryResponse(TransactionCategory category) {
        return new TransactionCategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getDirection(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.isActive(),
                category.isSystemCategory(),
                category.getDisplayOrder(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    public CashTransactionResponse toTransactionResponse(CashTransaction tx, BigDecimal balanceAfter, List<String> warnings) {
        BigDecimal income = tx.getDirection() == TransactionDirection.IN ? tx.getAmount() : BigDecimal.ZERO;
        BigDecimal expense = tx.getDirection() == TransactionDirection.OUT ? tx.getAmount() : BigDecimal.ZERO;
        return new CashTransactionResponse(
                tx.getId(),
                tx.getTransactionCode(),
                tx.getTransactionDate(),
                tx.getAccount().getId(),
                tx.getAccount().getCode(),
                tx.getAccount().getName(),
                tx.getDirection(),
                tx.getCategory() != null ? tx.getCategory().getId() : null,
                tx.getCategory() != null ? tx.getCategory().getCode() : null,
                tx.getCategory() != null ? tx.getCategory().getName() : null,
                tx.getAmount(),
                income,
                expense,
                balanceAfter,
                tx.getStatus(),
                tx.getSourceType(),
                tx.getSourceId(),
                tx.getReferenceNo(),
                tx.getPayerOrPayee(),
                tx.getDescription(),
                tx.getAttachmentReference(),
                tx.getTransferGroupId(),
                tx.getOriginalTransaction() != null ? tx.getOriginalTransaction().getId() : null,
                tx.getOriginalTransaction() != null ? tx.getOriginalTransaction().getTransactionCode() : null,
                tx.getReversalTransaction() != null ? tx.getReversalTransaction().getId() : null,
                tx.getReversalTransaction() != null ? tx.getReversalTransaction().getTransactionCode() : null,
                tx.getCancelReason() != null ? tx.getCancelReason() : tx.getReversalReason(),
                tx.getPostedAt(),
                tx.getCanceledAt() != null ? tx.getCanceledAt() : tx.getReversedAt(),
                tx.getCanceledBy(),
                tx.getCreatedAt(),
                tx.getCreatedBy(),
                tx.getUpdatedAt(),
                tx.getUpdatedBy(),
                warnings
        );
    }

    public FinancialPeriodResponse toPeriodResponse(FinancialPeriod period) {
        return new FinancialPeriodResponse(
                period.getId(),
                period.getYear(),
                period.getMonth(),
                period.getStatus(),
                period.getClosedAt(),
                period.getClosedBy(),
                period.getReopenedAt(),
                period.getReopenedBy(),
                period.getReopenReason(),
                period.getNote(),
                period.getOpeningBalanceSnapshot(),
                period.getTotalIncomeSnapshot(),
                period.getTotalExpenseSnapshot(),
                period.getClosingBalanceSnapshot(),
                period.getOutstandingDebtSnapshot(),
                period.getCreatedAt(),
                period.getUpdatedAt()
        );
    }
}
