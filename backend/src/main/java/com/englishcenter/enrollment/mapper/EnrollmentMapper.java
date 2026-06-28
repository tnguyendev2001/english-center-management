package com.englishcenter.enrollment.mapper;

import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.dto.EnrollmentResponse;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.mapper.InvoiceMapper;
import com.englishcenter.studentpackage.StudentPackage;
import com.englishcenter.studentpackage.mapper.StudentPackageMapper;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentMapper {
    private final StudentPackageMapper studentPackageMapper;
    private final InvoiceMapper invoiceMapper;

    public EnrollmentMapper(StudentPackageMapper studentPackageMapper, InvoiceMapper invoiceMapper) {
        this.studentPackageMapper = studentPackageMapper;
        this.invoiceMapper = invoiceMapper;
    }

    public EnrollmentResponse toResponse(Enrollment enrollment) {
        return toResponse(enrollment, null, null);
    }

    public EnrollmentResponse toResponse(
            Enrollment enrollment,
            StudentPackage studentPackage,
            Invoice invoice
    ) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getStudentCode(),
                enrollment.getStudent().getFullName(),
                enrollment.getClassroom().getId(),
                enrollment.getClassroom().getClassName(),
                enrollment.getStartDate(),
                enrollment.getEndDate(),
                enrollment.getStatus(),
                enrollment.getSelectedPackage().getId(),
                enrollment.getPackageNameSnapshot(),
                enrollment.getTotalSessionsSnapshot(),
                enrollment.getPackagePriceSnapshot(),
                enrollment.getDiscountAmount(),
                enrollment.getFinalAmount(),
                enrollment.getNote(),
                studentPackage == null ? null : studentPackageMapper.toResponse(studentPackage),
                invoice == null ? null : invoiceMapper.toResponse(invoice),
                enrollment.getCreatedAt(),
                enrollment.getUpdatedAt()
        );
    }
}
