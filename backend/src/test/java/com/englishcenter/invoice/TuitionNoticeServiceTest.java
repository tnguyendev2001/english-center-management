package com.englishcenter.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.englishcenter.classroom.Classroom;
import com.englishcenter.common.config.CenterProperties;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.invoice.TuitionPeriodResolver.TuitionPeriod;
import com.englishcenter.invoice.dto.TuitionNoticeResponse;
import com.englishcenter.student.Student;
import com.englishcenter.studentpackage.StudentPackage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TuitionNoticeServiceTest {
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private TuitionPeriodResolver tuitionPeriodResolver;

    @Test
    void assemblesParentNoticeFromInvoiceSnapshotsWithoutInternalNote() {
        Invoice invoice = invoice();
        CenterProperties centerProperties = new CenterProperties();
        TuitionNoticeService service = new TuitionNoticeService(
                invoiceRepository,
                tuitionPeriodResolver,
                centerProperties
        );

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(tuitionPeriodResolver.resolve(invoice))
                .thenReturn(new TuitionPeriod(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 27)));

        TuitionNoticeResponse notice = service.getByInvoiceId(1L);

        assertThat(notice.centerName()).isEqualTo("Anh ngữ KHAI PHÓNG");
        assertThat(notice.noticeDate()).isEqualTo(LocalDate.of(2026, 8, 27));
        assertThat(notice.classroomName()).isEqualTo("GRADE 7-A");
        assertThat(notice.invoiceAmount()).isEqualByComparingTo("500000");
        assertThat(notice.amountInWords()).isEqualTo("Năm trăm nghìn đồng");
        assertThat(notice.toString()).doesNotContain("Nhập liệu legacy Excel");
    }

    @Test
    void keepsFullTuitionAmountAndShowsPartialPaymentBreakdown() {
        Invoice invoice = invoice();
        invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        invoice.setPaidAmount(new BigDecimal("200000"));
        invoice.setRemainingAmount(new BigDecimal("300000"));
        TuitionNoticeService service = new TuitionNoticeService(
                invoiceRepository,
                tuitionPeriodResolver,
                new CenterProperties()
        );

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(tuitionPeriodResolver.resolve(invoice)).thenReturn(new TuitionPeriod(null, null));

        TuitionNoticeResponse notice = service.getByInvoiceId(1L);

        assertThat(notice.invoiceAmount()).isEqualByComparingTo("500000");
        assertThat(notice.paidAmount()).isEqualByComparingTo("200000");
        assertThat(notice.remainingAmount()).isEqualByComparingTo("300000");
        assertThat(notice.invoiceStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
    }

    private Invoice invoice() {
        Student student = new Student();
        student.setId(10L);
        student.setStudentCode("ST00062");
        student.setFullName("Trương Ngọc Diệp");

        Classroom originalClassroom = new Classroom();
        originalClassroom.setId(20L);
        originalClassroom.setClassName("GRADE 7-A");

        Enrollment enrollment = new Enrollment();
        enrollment.setId(30L);
        enrollment.setStudent(student);
        enrollment.setClassroom(originalClassroom);

        StudentPackage studentPackage = new StudentPackage();
        studentPackage.setId(40L);
        studentPackage.setEnrollment(enrollment);

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceCode("INV-964F256B");
        invoice.setStudent(student);
        invoice.setClassroom(originalClassroom);
        invoice.setEnrollment(enrollment);
        invoice.setStudentPackage(studentPackage);
        invoice.setPackageNameSnapshot("Gói 8 buổi");
        invoice.setTotalSessionsSnapshot(8);
        invoice.setFinalAmount(new BigDecimal("500000"));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setRemainingAmount(new BigDecimal("500000"));
        invoice.setDueDate(LocalDate.of(2026, 8, 4));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setNote("Nhập liệu legacy Excel");
        invoice.setCreatedAt(LocalDateTime.of(2026, 8, 27, 14, 17));
        return invoice;
    }
}
