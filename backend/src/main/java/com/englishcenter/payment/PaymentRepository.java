package com.englishcenter.payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(payment.amount), 0)
            FROM Payment payment
            WHERE payment.invoice.id = :invoiceId
              AND payment.status = com.englishcenter.payment.PaymentStatus.VALID
            """)
    BigDecimal sumValidAmountByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query("""
            SELECT COALESCE(SUM(payment.amount), 0)
            FROM Payment payment
            WHERE payment.status = com.englishcenter.payment.PaymentStatus.VALID
              AND payment.paymentDate BETWEEN :fromDate AND :toDate
            """)
    BigDecimal sumValidAmountBetween(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("""
            SELECT COALESCE(SUM(payment.amount), 0)
            FROM Payment payment
            WHERE payment.status = com.englishcenter.payment.PaymentStatus.VALID
            """)
    BigDecimal sumAllValidAmount();
}
