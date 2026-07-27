package com.englishcenter.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.common.config.AppTimeProperties;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.invoice.mapper.InvoiceMapper;
import com.englishcenter.student.Student;
import com.englishcenter.studentpackage.StudentPackage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceBillingEnrichmentTest {
    @Mock
    private ClassSessionRepository classSessionRepository;

    @Test
    void mapsBillingLabelCycleAndOverdueDays() {
        InvoiceEffectivePeriodService periodService = new InvoiceEffectivePeriodService(classSessionRepository);
        InvoiceMapper mapper = new InvoiceMapper(
                new BillingCycleLabelService(),
                periodService,
                new AppTimeProperties()
        );

        Invoice invoice = baseInvoice();
        invoice.setCycleNo(2);
        invoice.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        invoice.setDueDate(LocalDate.of(2026, 7, 1));
        invoice.setRemainingAmount(new BigDecimal("300000"));
        invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        invoice.setPackageNameSnapshot("Gói 8 buổi");
        invoice.setTotalSessionsSnapshot(8);
        invoice.setCreatedAt(LocalDateTime.of(2026, 7, 20, 10, 0));

        when(classSessionRepository.findByClassroomIdAndSessionDateBetweenAndStatusNotOrderBySessionDateAscStartTimeAsc(
                eq(5L),
                eq(LocalDate.of(2026, 6, 1)),
                any(LocalDate.class),
                eq(ClassSessionStatus.CANCELED)
        )).thenReturn(List.of());

        var response = mapper.toResponse(invoice);

        assertThat(response.billingLabel()).isEqualTo("Gói 8 buổi - Kỳ 2");
        assertThat(response.cycleNo()).isEqualTo(2);
        assertThat(response.effectiveFrom()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(response.estimatedEffectiveTo()).isNull();
        assertThat(response.overdueDays()).isGreaterThan(0);
        assertThat(response.debtStatus()).isEqualTo("OVERDUE");
    }

    @Test
    void estimatesEffectiveToFromExistingSessions() {
        InvoiceEffectivePeriodService periodService = new InvoiceEffectivePeriodService(classSessionRepository);
        Invoice invoice = baseInvoice();
        invoice.setCycleNo(1);
        invoice.setTotalSessionsSnapshot(2);
        invoice.setEffectiveFrom(LocalDate.of(2026, 6, 1));

        ClassSession first = new ClassSession();
        first.setSessionDate(LocalDate.of(2026, 6, 2));
        ClassSession second = new ClassSession();
        second.setSessionDate(LocalDate.of(2026, 6, 5));

        when(classSessionRepository.findByClassroomIdAndSessionDateBetweenAndStatusNotOrderBySessionDateAscStartTimeAsc(
                eq(5L),
                eq(LocalDate.of(2026, 6, 1)),
                any(LocalDate.class),
                eq(ClassSessionStatus.CANCELED)
        )).thenReturn(List.of(first, second));

        assertThat(periodService.estimateEffectiveTo(invoice)).isEqualTo(LocalDate.of(2026, 6, 5));
    }

    private Invoice baseInvoice() {
        Student student = new Student();
        student.setId(1L);
        student.setStudentCode("HV001");
        student.setFullName("Nguyen A");

        Classroom classroom = new Classroom();
        classroom.setId(5L);
        classroom.setClassCode("L1");
        classroom.setClassName("Starter");

        Enrollment enrollment = new Enrollment();
        enrollment.setId(9L);
        enrollment.setStartDate(LocalDate.of(2026, 6, 1));

        StudentPackage studentPackage = new StudentPackage();
        studentPackage.setId(3L);
        studentPackage.setCycleNo(2);
        studentPackage.setStartDate(LocalDate.of(2026, 8, 1));

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceCode("INV-1");
        invoice.setStudent(student);
        invoice.setClassroom(classroom);
        invoice.setEnrollment(enrollment);
        invoice.setStudentPackage(studentPackage);
        invoice.setAmount(new BigDecimal("500000"));
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setAdjustmentAmount(BigDecimal.ZERO);
        invoice.setFinalAmount(new BigDecimal("500000"));
        invoice.setPaidAmount(new BigDecimal("200000"));
        invoice.setRemainingAmount(new BigDecimal("300000"));
        invoice.setPackageNameSnapshot("Gói 8 buổi");
        invoice.setTotalSessionsSnapshot(8);
        invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        invoice.setDueDate(LocalDate.of(2026, 8, 8));
        invoice.setCreatedAt(LocalDateTime.of(2026, 8, 1, 9, 0));
        invoice.setUpdatedAt(LocalDateTime.of(2026, 8, 1, 9, 0));
        return invoice;
    }
}
