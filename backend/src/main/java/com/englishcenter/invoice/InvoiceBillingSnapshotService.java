package com.englishcenter.invoice;

import com.englishcenter.studentpackage.StudentPackage;
import com.englishcenter.tuitionpackage.TuitionPackage;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared billing-cycle snapshot builder used by Enrollment, Renew, and PackageChange flows.
 * Does not create invoices itself — callers own persistence and money calculation.
 */
@Service
public class InvoiceBillingSnapshotService {
    private final InvoiceDueDateService invoiceDueDateService;

    public InvoiceBillingSnapshotService(InvoiceDueDateService invoiceDueDateService) {
        this.invoiceDueDateService = invoiceDueDateService;
    }

    @Transactional(readOnly = true)
    public void applyBillingSnapshot(
            Invoice invoice,
            StudentPackage studentPackage,
            TuitionPackage tuitionPackage,
            LocalDate issueDate
    ) {
        applyBillingSnapshot(invoice, studentPackage, tuitionPackage, issueDate, null);
    }

    @Transactional(readOnly = true)
    public void applyBillingSnapshot(
            Invoice invoice,
            StudentPackage studentPackage,
            TuitionPackage tuitionPackage,
            LocalDate issueDate,
            LocalDate overrideDueDate
    ) {
        if (studentPackage != null) {
            invoice.setCycleNo(studentPackage.getCycleNo());
            invoice.setEffectiveFrom(studentPackage.getStartDate());
            invoice.setStudentPackage(studentPackage);
        }
        if (tuitionPackage != null) {
            invoice.setPackageId(tuitionPackage.getId());
            invoice.setPackageNameSnapshot(tuitionPackage.getName());
            invoice.setTotalSessionsSnapshot(tuitionPackage.getTotalSessions());
            invoice.setPackagePriceSnapshot(tuitionPackage.getPrice());
        } else if (studentPackage != null) {
            invoice.setPackageId(studentPackage.getTuitionPackage() != null
                    ? studentPackage.getTuitionPackage().getId()
                    : null);
            invoice.setPackageNameSnapshot(studentPackage.getPackageName());
            invoice.setTotalSessionsSnapshot(studentPackage.getTotalSessions());
            invoice.setPackagePriceSnapshot(studentPackage.getPrice());
        }

        if (invoice.getPackagePriceSnapshot() == null) {
            invoice.setPackagePriceSnapshot(
                    invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO
            );
        }

        if (overrideDueDate != null) {
            invoice.setDueDate(overrideDueDate);
        } else {
            invoice.setDueDate(invoiceDueDateService.calculateDueDate(issueDate));
        }
    }
}
