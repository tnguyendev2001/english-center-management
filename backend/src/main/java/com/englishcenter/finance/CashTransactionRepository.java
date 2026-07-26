package com.englishcenter.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashTransactionRepository extends JpaRepository<CashTransaction, Long>, JpaSpecificationExecutor<CashTransaction> {
    Optional<CashTransaction> findByTransactionCode(String transactionCode);

    List<CashTransaction> findByTransferGroupIdOrderByIdAsc(String transferGroupId);

    Optional<CashTransaction> findFirstBySourceTypeAndSourceIdAndOriginalTransactionIsNull(
            TransactionSourceType sourceType,
            Long sourceId
    );

    List<CashTransaction> findAllBySourceTypeAndSourceId(TransactionSourceType sourceType, Long sourceId);

    long countBySourceTypeAndSourceIdAndOriginalTransactionIsNull(
            TransactionSourceType sourceType,
            Long sourceId
    );

    @Query("""
            SELECT COALESCE(SUM(tx.amount), 0)
            FROM CashTransaction tx
            WHERE tx.account.id = :accountId
              AND tx.status = com.englishcenter.finance.TransactionStatus.POSTED
              AND tx.direction = :direction
              AND tx.transactionDate <= :asOfDate
            """)
    BigDecimal sumFinalizedAmountByAccountAndDirectionUpTo(
            @Param("accountId") Long accountId,
            @Param("direction") TransactionDirection direction,
            @Param("asOfDate") LocalDate asOfDate
    );

    @Query("""
            SELECT COALESCE(SUM(tx.amount), 0)
            FROM CashTransaction tx
            WHERE tx.status = com.englishcenter.finance.TransactionStatus.POSTED
              AND tx.direction = :direction
              AND tx.sourceType <> com.englishcenter.finance.TransactionSourceType.TRANSFER
              AND tx.transactionDate >= :fromDate
              AND tx.transactionDate <= :toDate
              AND tx.account.id = COALESCE(:accountId, tx.account.id)
              AND COALESCE(tx.category.id, -1L) = COALESCE(:categoryId, COALESCE(tx.category.id, -1L))
            """)
    BigDecimal sumPeriodAmountExcludingTransfers(
            @Param("direction") TransactionDirection direction,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId
    );

    @Query("""
            SELECT COALESCE(SUM(tx.amount), 0)
            FROM CashTransaction tx
            WHERE tx.status = com.englishcenter.finance.TransactionStatus.POSTED
              AND tx.direction = com.englishcenter.finance.TransactionDirection.IN
              AND tx.sourceType = com.englishcenter.finance.TransactionSourceType.PAYMENT
              AND tx.transactionDate >= :fromDate
              AND tx.transactionDate <= :toDate
            """)
    BigDecimal sumTuitionLedgerAmount(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("""
            SELECT tx
            FROM CashTransaction tx
            WHERE tx.status = com.englishcenter.finance.TransactionStatus.POSTED
              AND tx.transactionDate >= :fromDate
              AND tx.transactionDate <= :toDate
            ORDER BY tx.transactionDate ASC, tx.id ASC
            """)
    List<CashTransaction> findFinalizedInRange(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("""
            SELECT tx
            FROM CashTransaction tx
            WHERE tx.status = com.englishcenter.finance.TransactionStatus.POSTED
              AND tx.sourceType <> com.englishcenter.finance.TransactionSourceType.TRANSFER
              AND tx.transactionDate >= :fromDate
              AND tx.transactionDate <= :toDate
            ORDER BY tx.transactionDate ASC, tx.id ASC
            """)
    List<CashTransaction> findFinalizedNonTransferInRange(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    List<CashTransaction> findTop20ByOrderByTransactionDateDescIdDesc();

    @Query("""
            SELECT MAX(tx.transactionDate)
            FROM CashTransaction tx
            WHERE tx.status = com.englishcenter.finance.TransactionStatus.POSTED
            """)
    LocalDate findLatestPostedTransactionDate();

    @Query(value = """
            SELECT DISTINCT TO_CHAR(transaction_date, 'YYYY-MM')
            FROM cash_transactions
            WHERE status = 'POSTED'
            ORDER BY 1
            """, nativeQuery = true)
    List<String> findPostedTransactionYearMonths();

    @Query("""
            SELECT COUNT(tx)
            FROM CashTransaction tx
            WHERE tx.status = com.englishcenter.finance.TransactionStatus.POSTED
              AND tx.transactionDate >= :fromDate
              AND tx.transactionDate <= :toDate
            """)
    long countPostedInRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    Optional<CashTransaction> findTopByAccountIdAndStatusInOrderByTransactionDateDescIdDesc(
            Long accountId,
            Collection<TransactionStatus> statuses
    );

    @Query("""
            SELECT tx
            FROM CashTransaction tx
            WHERE tx.sourceType = com.englishcenter.finance.TransactionSourceType.PAYMENT
              AND tx.originalTransaction IS NULL
            ORDER BY tx.transactionDate ASC, tx.id ASC
            """)
    List<CashTransaction> findAllPaymentLedgerRows();

    @Query("""
            SELECT COALESCE(SUM(tx.amount), 0)
            FROM CashTransaction tx
            WHERE tx.category.id = :categoryId
              AND tx.status = com.englishcenter.finance.TransactionStatus.POSTED
              AND tx.direction = :direction
              AND tx.sourceType <> com.englishcenter.finance.TransactionSourceType.TRANSFER
              AND tx.transactionDate >= :fromDate
              AND tx.transactionDate <= :toDate
            """)
    BigDecimal sumByCategoryAndDirectionInRange(
            @Param("categoryId") Long categoryId,
            @Param("direction") TransactionDirection direction,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
