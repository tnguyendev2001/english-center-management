package com.englishcenter.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    long countByStatus(InvoiceStatus status);

    @Query("""
            SELECT COALESCE(SUM(invoice.remainingAmount), 0)
            FROM Invoice invoice
            WHERE invoice.status IN (
                com.englishcenter.invoice.InvoiceStatus.UNPAID,
                com.englishcenter.invoice.InvoiceStatus.PARTIALLY_PAID
            )
            """)
    BigDecimal sumDebtAmount();

    @Query("""
            SELECT COALESCE(SUM(invoice.remainingAmount), 0)
            FROM Invoice invoice
            WHERE invoice.classroom.id = :classroomId
              AND invoice.status IN (
                com.englishcenter.invoice.InvoiceStatus.UNPAID,
                com.englishcenter.invoice.InvoiceStatus.PARTIALLY_PAID
              )
            """)
    BigDecimal sumDebtAmountByClassroomId(@Param("classroomId") Long classroomId);

    @Query("""
            SELECT COUNT(DISTINCT invoice.student.id)
            FROM Invoice invoice
            WHERE invoice.status IN (
                com.englishcenter.invoice.InvoiceStatus.UNPAID,
                com.englishcenter.invoice.InvoiceStatus.PARTIALLY_PAID
            )
            """)
    long countDistinctStudentsWithDebt();

    Page<Invoice> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<Invoice> findByEnrollmentId(Long enrollmentId);

    @Query("""
            SELECT invoice
            FROM Invoice invoice
            WHERE (:status IS NULL OR invoice.status = :status)
              AND (:studentId IS NULL OR invoice.student.id = :studentId)
              AND (:classroomId IS NULL OR invoice.classroom.id = :classroomId)
            ORDER BY invoice.createdAt DESC
            """)
    Page<Invoice> search(
            @Param("status") InvoiceStatus status,
            @Param("studentId") Long studentId,
            @Param("classroomId") Long classroomId,
            Pageable pageable
    );

    @Query("""
            SELECT invoice
            FROM Invoice invoice
            WHERE (:status IS NULL OR invoice.status = :status)
              AND (:classroomId IS NULL OR invoice.classroom.id = :classroomId)
              AND (:fromDate IS NULL OR CAST(invoice.createdAt AS localdate) >= :fromDate)
              AND (:toDate IS NULL OR CAST(invoice.createdAt AS localdate) <= :toDate)
              AND (
                  :keyword IS NULL OR :keyword = ''
                  OR LOWER(invoice.student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(invoice.invoiceCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY invoice.createdAt DESC
            """)
    Page<Invoice> searchInvoiceReport(
            @Param("status") InvoiceStatus status,
            @Param("classroomId") Long classroomId,
            @Param("keyword") String keyword,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    @Query("""
            SELECT invoice
            FROM Invoice invoice
            WHERE invoice.status IN (
                com.englishcenter.invoice.InvoiceStatus.UNPAID,
                com.englishcenter.invoice.InvoiceStatus.PARTIALLY_PAID
            )
            ORDER BY invoice.createdAt DESC
            """)
    Page<Invoice> findDebtInvoices(Pageable pageable);

    @Query("""
            SELECT invoice
            FROM Invoice invoice
            WHERE invoice.status IN (
                com.englishcenter.invoice.InvoiceStatus.UNPAID,
                com.englishcenter.invoice.InvoiceStatus.PARTIALLY_PAID
            )
              AND (:status IS NULL OR invoice.status = :status)
              AND (:classroomId IS NULL OR invoice.classroom.id = :classroomId)
              AND (:fromDate IS NULL OR CAST(invoice.createdAt AS localdate) >= :fromDate)
              AND (:toDate IS NULL OR CAST(invoice.createdAt AS localdate) <= :toDate)
              AND (
                  :keyword IS NULL OR :keyword = ''
                  OR LOWER(invoice.student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(invoice.invoiceCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY invoice.createdAt DESC
            """)
    Page<Invoice> searchDebtReport(
            @Param("status") InvoiceStatus status,
            @Param("classroomId") Long classroomId,
            @Param("keyword") String keyword,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    @Modifying
    @Query("""
            UPDATE Invoice invoice
            SET invoice.status = com.englishcenter.invoice.InvoiceStatus.REPLACED,
                invoice.updatedAt = :updatedAt
            WHERE invoice.studentPackage.id = :studentPackageId
              AND invoice.status IN (
                com.englishcenter.invoice.InvoiceStatus.UNPAID,
                com.englishcenter.invoice.InvoiceStatus.PARTIALLY_PAID
              )
            """)
    int replaceCollectibleInvoicesByStudentPackageId(
            @Param("studentPackageId") Long studentPackageId,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
