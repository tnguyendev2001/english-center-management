package com.englishcenter.payment;

import com.englishcenter.payment.PaymentMethod;
import com.englishcenter.payment.PaymentStatus;
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
            SELECT payment
            FROM Payment payment
            WHERE (:status IS NULL OR payment.status = :status)
              AND (:fromDate IS NULL OR payment.paymentDate >= :fromDate)
              AND (:toDate IS NULL OR payment.paymentDate <= :toDate)
              AND (:classroomId IS NULL OR payment.classroom.id = :classroomId)
              AND (:method IS NULL OR payment.method = :method)
              AND (
                  :keyword IS NULL OR :keyword = ''
                  OR LOWER(payment.student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(payment.paymentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(payment.invoice.invoiceCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY payment.paymentDate DESC, payment.createdAt DESC
            """)
    Page<Payment> searchPayments(
            @Param("status") PaymentStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("classroomId") Long classroomId,
            @Param("method") PaymentMethod method,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(payment)
            FROM Payment payment
            WHERE payment.status = com.englishcenter.payment.PaymentStatus.VALID
              AND (:fromDate IS NULL OR payment.paymentDate >= :fromDate)
              AND (:toDate IS NULL OR payment.paymentDate <= :toDate)
              AND (:classroomId IS NULL OR payment.classroom.id = :classroomId)
              AND (:method IS NULL OR payment.method = :method)
              AND (
                  :keyword IS NULL OR :keyword = ''
                  OR LOWER(payment.student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(payment.paymentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(payment.invoice.invoiceCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    long countValidPaymentsFiltered(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("classroomId") Long classroomId,
            @Param("method") PaymentMethod method,
            @Param("keyword") String keyword
    );

    @Query("""
            SELECT COALESCE(SUM(payment.amount), 0)
            FROM Payment payment
            WHERE payment.status = com.englishcenter.payment.PaymentStatus.VALID
              AND payment.method = :method
              AND (:fromDate IS NULL OR payment.paymentDate >= :fromDate)
              AND (:toDate IS NULL OR payment.paymentDate <= :toDate)
              AND (:classroomId IS NULL OR payment.classroom.id = :classroomId)
              AND (
                  :keyword IS NULL OR :keyword = ''
                  OR LOWER(payment.student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(payment.paymentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(payment.invoice.invoiceCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    BigDecimal sumValidAmountByMethodFiltered(
            @Param("method") PaymentMethod method,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("classroomId") Long classroomId,
            @Param("keyword") String keyword
    );

    @Query("""
            SELECT payment
            FROM Payment payment
            WHERE payment.status = com.englishcenter.payment.PaymentStatus.VALID
            ORDER BY payment.paymentDate DESC, payment.createdAt DESC
            """)
    Page<Payment> findRecentValidPayments(Pageable pageable);

    @Query("""
            SELECT payment
            FROM Payment payment
            WHERE payment.status = com.englishcenter.payment.PaymentStatus.VALID
              AND (:fromDate IS NULL OR payment.paymentDate >= :fromDate)
              AND (:toDate IS NULL OR payment.paymentDate <= :toDate)
              AND (:classroomId IS NULL OR payment.classroom.id = :classroomId)
              AND (:method IS NULL OR payment.method = :method)
            ORDER BY payment.paymentDate DESC, payment.createdAt DESC
            """)
    Page<Payment> searchValidPayments(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("classroomId") Long classroomId,
            @Param("method") PaymentMethod method,
            Pageable pageable
    );

    @Query("""
            SELECT payment.paymentDate, COALESCE(SUM(payment.amount), 0)
            FROM Payment payment
            WHERE payment.status = com.englishcenter.payment.PaymentStatus.VALID
              AND (:fromDate IS NULL OR payment.paymentDate >= :fromDate)
              AND (:toDate IS NULL OR payment.paymentDate <= :toDate)
              AND (:classroomId IS NULL OR payment.classroom.id = :classroomId)
              AND (:method IS NULL OR payment.method = :method)
            GROUP BY payment.paymentDate
            ORDER BY payment.paymentDate ASC
            """)
    java.util.List<Object[]> sumValidAmountGroupByPaymentDate(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("classroomId") Long classroomId,
            @Param("method") PaymentMethod method
    );

    @Query("""
            SELECT payment.method, COALESCE(SUM(payment.amount), 0)
            FROM Payment payment
            WHERE payment.status = com.englishcenter.payment.PaymentStatus.VALID
              AND (:fromDate IS NULL OR payment.paymentDate >= :fromDate)
              AND (:toDate IS NULL OR payment.paymentDate <= :toDate)
              AND (:classroomId IS NULL OR payment.classroom.id = :classroomId)
              AND (:method IS NULL OR payment.method = :method)
            GROUP BY payment.method
            ORDER BY payment.method ASC
            """)
    java.util.List<Object[]> sumValidAmountGroupByMethod(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("classroomId") Long classroomId,
            @Param("method") PaymentMethod method
    );

    @Query("""
            SELECT COALESCE(SUM(payment.amount), 0)
            FROM Payment payment
            WHERE payment.status = com.englishcenter.payment.PaymentStatus.VALID
              AND (:fromDate IS NULL OR payment.paymentDate >= :fromDate)
              AND (:toDate IS NULL OR payment.paymentDate <= :toDate)
              AND (:classroomId IS NULL OR payment.classroom.id = :classroomId)
              AND (:method IS NULL OR payment.method = :method)
              AND (
                  :keyword IS NULL OR :keyword = ''
                  OR LOWER(payment.student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(payment.paymentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(payment.invoice.invoiceCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    BigDecimal sumValidAmountFiltered(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("classroomId") Long classroomId,
            @Param("method") PaymentMethod method,
            @Param("keyword") String keyword
    );

    @Query("""
            SELECT COALESCE(SUM(payment.amount), 0)
            FROM Payment payment
            WHERE payment.status = com.englishcenter.payment.PaymentStatus.VALID
              AND payment.classroom.id = :classroomId
              AND payment.paymentDate BETWEEN :fromDate AND :toDate
            """)
    BigDecimal sumValidAmountByClassroomBetween(
            @Param("classroomId") Long classroomId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

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
              AND payment.invoice.id IN (
                  SELECT invoice.id
                  FROM Invoice invoice
                  WHERE invoice.studentPackage.id = :studentPackageId
              )
            """)
    BigDecimal sumValidAmountByStudentPackageId(@Param("studentPackageId") Long studentPackageId);

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
