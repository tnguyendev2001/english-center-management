package com.englishcenter.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.common.config.AppTimeProperties;
import com.englishcenter.enrollment.dto.CanceledEnrollmentInvoiceDiagnosticResponse;
import com.englishcenter.enrollment.dto.CanceledEnrollmentInvoiceRepairResponse;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.payment.PaymentStatus;
import com.englishcenter.student.Student;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CanceledEnrollmentInvoiceConsistencyServiceTest {
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private AttendanceRepository attendanceRepository;

    @Test
    void diagnoseMarksSafeAndUnsafeRows() {
        CanceledEnrollmentInvoiceConsistencyService service = newService();
        Invoice safe = inconsistentInvoice(10L, 100L, InvoiceStatus.UNPAID);
        Invoice unsafe = inconsistentInvoice(11L, 101L, InvoiceStatus.PARTIALLY_PAID);
        when(invoiceRepository.findCollectibleInvoicesLinkedToCanceledEnrollments())
                .thenReturn(List.of(safe, unsafe));
        when(paymentRepository.countByInvoiceIdAndStatus(100L, PaymentStatus.VALID)).thenReturn(0L);
        when(paymentRepository.countByInvoiceIdAndStatus(101L, PaymentStatus.VALID)).thenReturn(1L);
        when(attendanceRepository.countForEnrollmentPeriod(
                1L, 2L, LocalDate.of(2026, 7, 1), null
        )).thenReturn(0L);

        List<CanceledEnrollmentInvoiceDiagnosticResponse> rows = service.diagnose();

        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst().safeToRepair()).isTrue();
        assertThat(rows.get(1).safeToRepair()).isFalse();
        assertThat(rows.get(1).validPaymentCount()).isEqualTo(1);
    }

    @Test
    void repairDryRunDoesNotPersist() {
        CanceledEnrollmentInvoiceConsistencyService service = newService();
        Invoice safe = inconsistentInvoice(10L, 100L, InvoiceStatus.UNPAID);
        when(invoiceRepository.findCollectibleInvoicesLinkedToCanceledEnrollments())
                .thenReturn(List.of(safe));
        when(paymentRepository.countByInvoiceIdAndStatus(100L, PaymentStatus.VALID)).thenReturn(0L);
        when(attendanceRepository.countForEnrollmentPeriod(
                1L, 2L, LocalDate.of(2026, 7, 1), null
        )).thenReturn(0L);

        CanceledEnrollmentInvoiceRepairResponse response = service.repair(true);

        assertThat(response.dryRun()).isTrue();
        assertThat(response.safeCount()).isEqualTo(1);
        assertThat(response.repairedCount()).isZero();
        assertThat(response.repairedInvoiceIds()).containsExactly(100L);
        verify(invoiceRepository, never()).findByIdForUpdate(anyLong());
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void repairAppliesOnlySafeRows() {
        CanceledEnrollmentInvoiceConsistencyService service = newService();
        Invoice safe = inconsistentInvoice(10L, 100L, InvoiceStatus.UNPAID);
        Invoice unsafe = inconsistentInvoice(11L, 101L, InvoiceStatus.UNPAID);
        when(invoiceRepository.findCollectibleInvoicesLinkedToCanceledEnrollments())
                .thenReturn(List.of(safe, unsafe));
        when(paymentRepository.countByInvoiceIdAndStatus(100L, PaymentStatus.VALID)).thenReturn(0L);
        when(paymentRepository.countByInvoiceIdAndStatus(101L, PaymentStatus.VALID)).thenReturn(1L);
        when(attendanceRepository.countForEnrollmentPeriod(
                1L, 2L, LocalDate.of(2026, 7, 1), null
        )).thenReturn(0L);
        when(invoiceRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(safe));
        when(paymentRepository.existsByInvoiceIdAndStatus(100L, PaymentStatus.VALID)).thenReturn(false);

        CanceledEnrollmentInvoiceRepairResponse response = service.repair(false);

        assertThat(response.repairedCount()).isEqualTo(1);
        assertThat(response.repairedInvoiceIds()).containsExactly(100L);
        assertThat(response.unsafeCount()).isEqualTo(1);
        assertThat(safe.getStatus()).isEqualTo(InvoiceStatus.CANCELED);
        assertThat(safe.getCancelReason()).contains("repair");
        assertThat(unsafe.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        verify(invoiceRepository).save(safe);
        verify(invoiceRepository, never()).findByIdForUpdate(101L);
    }

    private CanceledEnrollmentInvoiceConsistencyService newService() {
        return new CanceledEnrollmentInvoiceConsistencyService(
                invoiceRepository,
                paymentRepository,
                attendanceRepository,
                new AppTimeProperties()
        );
    }

    private Invoice inconsistentInvoice(Long enrollmentId, Long invoiceId, InvoiceStatus status) {
        Student student = new Student();
        student.setId(1L);
        Classroom classroom = new Classroom();
        classroom.setId(2L);

        Enrollment enrollment = new Enrollment();
        enrollment.setId(enrollmentId);
        enrollment.setStudent(student);
        enrollment.setClassroom(classroom);
        enrollment.setStartDate(LocalDate.of(2026, 7, 1));
        enrollment.setStatus(EnrollmentStatus.CANCELED);

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setEnrollment(enrollment);
        invoice.setStudent(student);
        invoice.setClassroom(classroom);
        invoice.setStatus(status);
        invoice.setFinalAmount(new BigDecimal("500000"));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setRemainingAmount(new BigDecimal("500000"));
        return invoice;
    }
}
