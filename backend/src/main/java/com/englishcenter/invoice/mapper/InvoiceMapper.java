package com.englishcenter.invoice.mapper;

import com.englishcenter.classroom.Classroom;
import com.englishcenter.common.config.AppTimeProperties;
import com.englishcenter.invoice.BillingCycleLabelService;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceEffectivePeriodService;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.student.Student;
import com.englishcenter.studentpackage.StudentPackage;
import com.englishcenter.studentpackage.StudentPackageSourceType;
import com.englishcenter.tuitionpackage.TuitionPackage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper {
    private final BillingCycleLabelService billingCycleLabelService;
    private final InvoiceEffectivePeriodService effectivePeriodService;
    private final AppTimeProperties appTimeProperties;

    public InvoiceMapper(
            BillingCycleLabelService billingCycleLabelService,
            InvoiceEffectivePeriodService effectivePeriodService,
            AppTimeProperties appTimeProperties
    ) {
        this.billingCycleLabelService = billingCycleLabelService;
        this.effectivePeriodService = effectivePeriodService;
        this.appTimeProperties = appTimeProperties;
    }

    public InvoiceResponse toResponse(Invoice invoice) {
        return toResponse(invoice, true);
    }

    public InvoiceResponse toResponse(Invoice invoice, boolean computeEstimatedEnd) {
        Student student = invoice.getStudent();
        Classroom classroom = invoice.getClassroom();
        StudentPackage studentPackage = invoice.getStudentPackage();
        TuitionPackage tuitionPackage = studentPackage != null ? studentPackage.getTuitionPackage() : null;

        Integer cycleNo = invoice.getCycleNo() != null
                ? invoice.getCycleNo()
                : (studentPackage != null ? studentPackage.getCycleNo() : null);
        LocalDate effectiveFrom = effectivePeriodService.resolveEffectiveFrom(invoice);
        LocalDate estimatedEffectiveTo = computeEstimatedEnd
                ? effectivePeriodService.estimateEffectiveTo(invoice)
                : null;
        LocalDate issueDate = invoice.getCreatedAt() != null
                ? invoice.getCreatedAt().atZone(appTimeProperties.zoneId()).toLocalDate()
                : null;
        LocalDate businessToday = LocalDate.now(appTimeProperties.zoneId());
        int overdueDays = computeOverdueDays(invoice, businessToday);
        String debtStatus = resolveDebtStatus(invoice, overdueDays, businessToday);
        BigDecimal packagePriceSnapshot = invoice.getPackagePriceSnapshot() != null
                ? invoice.getPackagePriceSnapshot()
                : invoice.getAmount();
        Long packageId = invoice.getPackageId() != null
                ? invoice.getPackageId()
                : (tuitionPackage != null ? tuitionPackage.getId() : null);
        String source = resolveSource(studentPackage);

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceCode(),
                student.getId(),
                student.getStudentCode(),
                student.getFullName(),
                classroom.getId(),
                classroom.getClassCode(),
                classroom.getClassName(),
                resolveTeacherName(classroom),
                invoice.getEnrollment().getId(),
                studentPackage != null ? studentPackage.getId() : null,
                packageId,
                null,
                invoice.getPackageNameSnapshot(),
                invoice.getTotalSessionsSnapshot(),
                packagePriceSnapshot,
                cycleNo,
                billingCycleLabelService.buildBillingLabel(invoice.getPackageNameSnapshot(), cycleNo),
                effectiveFrom,
                estimatedEffectiveTo,
                invoice.getAmount(),
                invoice.getDiscountAmount(),
                invoice.getAdjustmentAmount(),
                invoice.getFinalAmount(),
                invoice.getPaidAmount(),
                invoice.getRemainingAmount(),
                issueDate,
                invoice.getDueDate(),
                overdueDays,
                debtStatus,
                invoice.getStatus(),
                invoice.getNote(),
                source,
                invoice.getCreatedAt(),
                invoice.getUpdatedAt()
        );
    }

    public int computeOverdueDays(Invoice invoice, LocalDate businessToday) {
        if (invoice.getDueDate() == null
                || invoice.getRemainingAmount() == null
                || invoice.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0
                || invoice.getStatus() == InvoiceStatus.CANCELED
                || invoice.getStatus() == InvoiceStatus.PAID
                || invoice.getStatus() == InvoiceStatus.REPLACED) {
            return 0;
        }
        if (!invoice.getDueDate().isBefore(businessToday)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(invoice.getDueDate(), businessToday);
    }

    private String resolveDebtStatus(Invoice invoice, int overdueDays, LocalDate businessToday) {
        if (invoice.getRemainingAmount() == null
                || invoice.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0
                || invoice.getStatus() == InvoiceStatus.PAID
                || invoice.getStatus() == InvoiceStatus.CANCELED
                || invoice.getStatus() == InvoiceStatus.REPLACED) {
            return null;
        }
        if (overdueDays > 0) {
            return "OVERDUE";
        }
        if (invoice.getDueDate() != null && invoice.getDueDate().isEqual(businessToday)) {
            return "DUE_TODAY";
        }
        if (invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID) {
            return "PARTIALLY_PAID";
        }
        return "NOT_DUE";
    }

    private String resolveTeacherName(Classroom classroom) {
        if (classroom.getTeacher() != null && classroom.getTeacher().getFullName() != null) {
            return classroom.getTeacher().getFullName();
        }
        return classroom.getTeacherName();
    }

    private String resolveSource(StudentPackage studentPackage) {
        if (studentPackage == null || studentPackage.getSourceType() == null) {
            return null;
        }
        StudentPackageSourceType sourceType = studentPackage.getSourceType();
        return sourceType.name();
    }
}
