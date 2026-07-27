package com.englishcenter.enrollment;

import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.common.config.AppTimeProperties;
import com.englishcenter.enrollment.dto.CanceledEnrollmentInvoiceDiagnosticResponse;
import com.englishcenter.enrollment.dto.CanceledEnrollmentInvoiceRepairResponse;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceDebtSupport;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.payment.PaymentStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Detects and safely repairs invoices that remain collectible after enrollment cancellation.
 *
 * <p>Preview (dry-run):
 * {@code GET /api/enrollments/canceled-invoice-consistency}
 *
 * <p>Repair:
 * {@code POST /api/enrollments/canceled-invoice-consistency/repair?dryRun=true|false}
 *
 * <p>Only ADMIN may call these endpoints. Rows with attendance or VALID payments are never auto-repaired.
 */
@Service
public class CanceledEnrollmentInvoiceConsistencyService {
    private static final String REPAIR_REASON = "Đồng bộ hóa đơn sau khi hủy ghi danh (repair)";

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AppTimeProperties appTimeProperties;

    public CanceledEnrollmentInvoiceConsistencyService(
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            AttendanceRepository attendanceRepository,
            AppTimeProperties appTimeProperties
    ) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.attendanceRepository = attendanceRepository;
        this.appTimeProperties = appTimeProperties;
    }

    @Transactional(readOnly = true)
    public List<CanceledEnrollmentInvoiceDiagnosticResponse> diagnose() {
        return buildDiagnostics(invoiceRepository.findCollectibleInvoicesLinkedToCanceledEnrollments());
    }

    @Transactional
    public CanceledEnrollmentInvoiceRepairResponse repair(boolean dryRun) {
        List<CanceledEnrollmentInvoiceDiagnosticResponse> diagnostics = diagnose();
        List<Long> repairedInvoiceIds = new ArrayList<>();
        List<CanceledEnrollmentInvoiceDiagnosticResponse> unsafeRows = new ArrayList<>();

        LocalDateTime canceledAt = LocalDateTime.now(appTimeProperties.zoneId());
        for (CanceledEnrollmentInvoiceDiagnosticResponse row : diagnostics) {
            if (!row.safeToRepair()) {
                unsafeRows.add(row);
                continue;
            }
            if (dryRun) {
                repairedInvoiceIds.add(row.invoiceId());
                continue;
            }

            Invoice invoice = invoiceRepository.findByIdForUpdate(row.invoiceId()).orElse(null);
            if (invoice == null || !InvoiceDebtSupport.isCollectible(invoice)) {
                continue;
            }
            if (invoice.getEnrollment() == null
                    || invoice.getEnrollment().getStatus() != EnrollmentStatus.CANCELED) {
                continue;
            }
            if (paymentRepository.existsByInvoiceIdAndStatus(invoice.getId(), PaymentStatus.VALID)) {
                unsafeRows.add(row);
                continue;
            }

            long attendanceCount = attendanceRepository.countForEnrollmentPeriod(
                    invoice.getEnrollment().getStudent().getId(),
                    invoice.getEnrollment().getClassroom().getId(),
                    invoice.getEnrollment().getStartDate(),
                    invoice.getEnrollment().getEndDate()
            );
            if (attendanceCount > 0) {
                unsafeRows.add(row);
                continue;
            }

            invoice.setStatus(InvoiceStatus.CANCELED);
            invoice.setCancelReason(REPAIR_REASON);
            invoice.setCanceledAt(canceledAt);
            invoiceRepository.save(invoice);
            repairedInvoiceIds.add(invoice.getId());
        }

        int safeCount = (int) diagnostics.stream().filter(CanceledEnrollmentInvoiceDiagnosticResponse::safeToRepair).count();
        return new CanceledEnrollmentInvoiceRepairResponse(
                dryRun,
                diagnostics.size(),
                safeCount,
                unsafeRows.size(),
                dryRun ? 0 : repairedInvoiceIds.size(),
                List.copyOf(repairedInvoiceIds),
                List.copyOf(unsafeRows)
        );
    }

    private List<CanceledEnrollmentInvoiceDiagnosticResponse> buildDiagnostics(List<Invoice> invoices) {
        List<CanceledEnrollmentInvoiceDiagnosticResponse> result = new ArrayList<>();
        for (Invoice invoice : invoices) {
            Enrollment enrollment = invoice.getEnrollment();
            long validPaymentCount = paymentRepository.countByInvoiceIdAndStatus(
                    invoice.getId(),
                    PaymentStatus.VALID
            );
            long attendanceCount = attendanceRepository.countForEnrollmentPeriod(
                    enrollment.getStudent().getId(),
                    enrollment.getClassroom().getId(),
                    enrollment.getStartDate(),
                    enrollment.getEndDate()
            );
            boolean safeToRepair = validPaymentCount == 0 && attendanceCount == 0;
            result.add(new CanceledEnrollmentInvoiceDiagnosticResponse(
                    enrollment.getId(),
                    enrollment.getStudent().getId(),
                    enrollment.getClassroom().getId(),
                    enrollment.getStatus(),
                    invoice.getId(),
                    invoice.getStatus(),
                    invoice.getFinalAmount(),
                    invoice.getPaidAmount(),
                    invoice.getRemainingAmount(),
                    validPaymentCount,
                    attendanceCount,
                    safeToRepair
            ));
        }
        return result;
    }
}
