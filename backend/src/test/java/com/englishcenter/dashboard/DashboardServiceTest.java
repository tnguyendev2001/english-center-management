package com.englishcenter.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.dashboard.dto.DashboardSummaryResponse;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.makeupcredit.MakeupCreditRepository;
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.student.StudentStatus;
import com.englishcenter.classroom.ClassroomStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MakeupCreditRepository makeupCreditRepository;

    @Mock
    private ClassSessionRepository classSessionRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getSummary_aggregatesCountsFromRepositories() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate firstDayOfYear = today.withDayOfYear(1);

        when(studentRepository.countByStatus(StudentStatus.ACTIVE)).thenReturn(2L);
        when(classroomRepository.countByStatus(ClassroomStatus.ONGOING)).thenReturn(1L);
        when(enrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE)).thenReturn(2L);
        when(enrollmentRepository.countDepletedByStatus(EnrollmentStatus.ACTIVE)).thenReturn(1L);
        when(enrollmentRepository.countLowSessionsByStatus(EnrollmentStatus.ACTIVE, 2)).thenReturn(1L);
        when(invoiceRepository.countDistinctStudentsWithDebt()).thenReturn(1L);
        when(invoiceRepository.countByStatus(InvoiceStatus.UNPAID)).thenReturn(1L);
        when(invoiceRepository.countByStatus(InvoiceStatus.PARTIALLY_PAID)).thenReturn(1L);
        when(invoiceRepository.sumDebtAmount()).thenReturn(new BigDecimal("60000"));
        when(paymentRepository.sumValidAmountBetween(today, today)).thenReturn(new BigDecimal("40000"));
        when(paymentRepository.sumValidAmountBetween(firstDayOfMonth, today)).thenReturn(new BigDecimal("40000"));
        when(paymentRepository.sumValidAmountBetween(firstDayOfYear, today)).thenReturn(new BigDecimal("40000"));
        when(makeupCreditRepository.sumAllAvailableMakeupSessions(MakeupCreditStatus.AVAILABLE)).thenReturn(3);
        when(classSessionRepository.countBySessionDateAndStatusNot(today, ClassSessionStatus.CANCELED)).thenReturn(2);
        when(classSessionRepository.countBySessionDateBetweenAndStatus(
                firstDayOfMonth,
                today,
                ClassSessionStatus.COMPLETED
        )).thenReturn(5);

        DashboardSummaryResponse summary = dashboardService.getSummary();

        assertThat(summary.totalActiveStudents()).isEqualTo(2L);
        assertThat(summary.totalActiveClassrooms()).isEqualTo(1L);
        assertThat(summary.totalActiveEnrollments()).isEqualTo(2L);
        assertThat(summary.totalStudentsWithDepletedSessions()).isEqualTo(1L);
        assertThat(summary.totalStudentsWithLowSessions()).isEqualTo(1L);
        assertThat(summary.totalStudentsWithDebt()).isEqualTo(1L);
        assertThat(summary.totalDebtAmount()).isEqualByComparingTo("60000");
        assertThat(summary.totalRevenueToday()).isEqualByComparingTo("40000");
        assertThat(summary.totalPendingMakeupCredits()).isEqualTo(3);
    }
}
