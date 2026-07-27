package com.englishcenter.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classroom.ClassroomStatus;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.dashboard.dto.DashboardAlertResponse;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentSessionService;
import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.finance.FinanceCalculationService;
import com.englishcenter.finance.FinancialPeriodService;
import com.englishcenter.finance.FinancialPeriodStatus;
import com.englishcenter.finance.ReconciliationStatus;
import com.englishcenter.finance.dto.AccountBalanceResponse;
import com.englishcenter.finance.dto.FinancialPeriodResponse;
import com.englishcenter.finance.dto.PaymentReconciliationResponse;
import com.englishcenter.finance.FinancialAccountType;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.security.CurrentUserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardAlertServiceTest {
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private EnrollmentSessionService enrollmentSessionService;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private ClassSessionRepository classSessionRepository;
    @Mock
    private FinanceCalculationService financeCalculationService;
    @Mock
    private FinancialPeriodService financialPeriodService;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private DashboardAlertService dashboardAlertService;

    @Test
    void getAdminAlerts_excludesZeroCountsAndIncludesCriticalAlerts() {
        LocalDate today = LocalDate.now();
        YearMonth previous = YearMonth.from(today).minusMonths(1);

        when(enrollmentRepository.countDepletedByStatus(EnrollmentStatus.ACTIVE)).thenReturn(3L);
        when(enrollmentRepository.countLowSessionsByStatus(EnrollmentStatus.ACTIVE, 2)).thenReturn(2L);
        when(classSessionRepository.countIncompleteAttendance(today)).thenReturn(0L);
        when(invoiceRepository.countOverdueInvoices(today)).thenReturn(8L);
        when(invoiceRepository.countStudentsWithMultipleUnpaidInvoices()).thenReturn(0L);
        when(classroomRepository.countByTeacherIdIsNullAndStatus(ClassroomStatus.ONGOING)).thenReturn(1L);
        when(financeCalculationService.calculateAccountBalances(today)).thenReturn(List.of(
                new AccountBalanceResponse(
                        1L, "CASH", "Cash", FinancialAccountType.CASH,
                        BigDecimal.ZERO, today, BigDecimal.ZERO, BigDecimal.TEN,
                        new BigDecimal("-10"), true, true, today
                )
        ));
        when(financeCalculationService.reconcilePayments(isNull(), isNull())).thenReturn(
                new PaymentReconciliationResponse(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        0, 0, 0, 0, 0, 0, 0, 0,
                        ReconciliationStatus.MATCHED,
                        List.of()
                )
        );
        when(financialPeriodService.getPeriod(previous.getYear(), previous.getMonthValue()))
                .thenReturn(new FinancialPeriodResponse(
                        null, previous.getYear(), previous.getMonthValue(),
                        FinancialPeriodStatus.CLOSED,
                        null, null, null, null, null, null, null, null, null, null, null, null, null
                ));

        List<DashboardAlertResponse> alerts = dashboardAlertService.getAdminAlerts();

        assertThat(alerts).extracting(DashboardAlertResponse::type)
                .contains(
                        DashboardAlertType.STUDENT_OUT_OF_SESSIONS,
                        DashboardAlertType.STUDENT_LOW_SESSIONS,
                        DashboardAlertType.OVERDUE_INVOICES,
                        DashboardAlertType.UNASSIGNED_CLASSROOM,
                        DashboardAlertType.NEGATIVE_FINANCIAL_ACCOUNT
                )
                .doesNotContain(
                        DashboardAlertType.SESSION_ATTENDANCE_NOT_COMPLETED,
                        DashboardAlertType.STUDENTS_WITH_MULTIPLE_UNPAID_INVOICES,
                        DashboardAlertType.PREVIOUS_MONTH_NOT_CLOSED
                );
        assertThat(alerts).allMatch(alert -> alert.count() > 0);
        assertThat(alerts.getFirst().severity()).isEqualTo(DashboardAlertSeverity.CRITICAL);
    }

    @Test
    void getTeacherAlerts_usesAssignedClassroomScopeOnly() {
        Long teacherId = 11L;
        LocalDate today = LocalDate.now();

        when(classSessionRepository.countTodayByTeacherId(teacherId, today)).thenReturn(2L);
        when(classSessionRepository.countIncompleteAttendanceByTeacherId(teacherId, today)).thenReturn(1L);
        when(enrollmentRepository.countDepletedByTeacherIdAndStatus(teacherId, EnrollmentStatus.ACTIVE))
                .thenReturn(4L);
        when(enrollmentRepository.countLowSessionsByTeacherIdAndStatus(teacherId, EnrollmentStatus.ACTIVE, 2))
                .thenReturn(0L);
        when(classSessionRepository.countUpcomingByTeacherId(teacherId, today)).thenReturn(5L);

        List<DashboardAlertResponse> alerts = dashboardAlertService.getTeacherAlerts(teacherId);

        assertThat(alerts).extracting(DashboardAlertResponse::type)
                .containsExactlyInAnyOrder(
                        DashboardAlertType.TODAY_ASSIGNED_CLASSES,
                        DashboardAlertType.TEACHER_SESSION_ATTENDANCE_NOT_COMPLETED,
                        DashboardAlertType.TEACHER_STUDENTS_OUT_OF_SESSIONS,
                        DashboardAlertType.UPCOMING_SESSIONS
                )
                .doesNotContain(
                        DashboardAlertType.NEGATIVE_FINANCIAL_ACCOUNT,
                        DashboardAlertType.OVERDUE_INVOICES,
                        DashboardAlertType.UNASSIGNED_CLASSROOM
                );
        assertThat(alerts).noneMatch(alert ->
                alert.actionUrl() != null && alert.actionUrl().startsWith("/finance"));
    }

    @Test
    void getStudentAlerts_onlyOwnDebtAndSessions() {
        Long studentId = 22L;
        LocalDate today = LocalDate.now();

        when(enrollmentRepository.findByStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of());
        when(classSessionRepository.findByEnrolledStudentId(studentId)).thenReturn(List.of());
        when(invoiceRepository.findDebtInvoicesByStudentId(studentId)).thenReturn(List.of());
        when(invoiceRepository.countOverdueInvoicesByStudentId(studentId, today)).thenReturn(0L);

        List<DashboardAlertResponse> alerts = dashboardAlertService.getStudentAlerts(studentId);

        assertThat(alerts).isEmpty();
    }

    @Test
    void getAdminAlerts_resolvedIssueRemovesAlert() {
        LocalDate today = LocalDate.now();
        YearMonth previous = YearMonth.from(today).minusMonths(1);

        stubEmptyAdminDependencies(today, previous);
        when(enrollmentRepository.countDepletedByStatus(EnrollmentStatus.ACTIVE)).thenReturn(1L);

        assertThat(dashboardAlertService.getAdminAlerts())
                .extracting(DashboardAlertResponse::type)
                .contains(DashboardAlertType.STUDENT_OUT_OF_SESSIONS);

        when(enrollmentRepository.countDepletedByStatus(EnrollmentStatus.ACTIVE)).thenReturn(0L);

        assertThat(dashboardAlertService.getAdminAlerts())
                .extracting(DashboardAlertResponse::type)
                .doesNotContain(DashboardAlertType.STUDENT_OUT_OF_SESSIONS);
    }

    private void stubEmptyAdminDependencies(LocalDate today, YearMonth previous) {
        when(enrollmentRepository.countLowSessionsByStatus(EnrollmentStatus.ACTIVE, 2)).thenReturn(0L);
        when(classSessionRepository.countIncompleteAttendance(today)).thenReturn(0L);
        when(invoiceRepository.countOverdueInvoices(today)).thenReturn(0L);
        when(invoiceRepository.countStudentsWithMultipleUnpaidInvoices()).thenReturn(0L);
        when(classroomRepository.countByTeacherIdIsNullAndStatus(ClassroomStatus.ONGOING)).thenReturn(0L);
        when(financeCalculationService.calculateAccountBalances(today)).thenReturn(List.of());
        when(financeCalculationService.reconcilePayments(isNull(), isNull())).thenReturn(
                new PaymentReconciliationResponse(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        0, 0, 0, 0, 0, 0, 0, 0,
                        ReconciliationStatus.MATCHED,
                        List.of()
                )
        );
        when(financialPeriodService.getPeriod(eq(previous.getYear()), eq(previous.getMonthValue())))
                .thenReturn(new FinancialPeriodResponse(
                        null, previous.getYear(), previous.getMonthValue(),
                        FinancialPeriodStatus.CLOSED,
                        null, null, null, null, null, null, null, null, null, null, null, null, null
                ));
    }
}
