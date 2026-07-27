package com.englishcenter.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.classpackage.ClassPackageRepository;
import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classroom.ClassroomStatus;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.common.config.AppTimeProperties;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.payment.Payment;
import com.englishcenter.enrollment.dto.EnrollStudentRequest;
import com.englishcenter.enrollment.dto.CancelEnrollmentRequest;
import com.englishcenter.enrollment.dto.CancelEnrollmentResponse;
import com.englishcenter.enrollment.dto.StopEnrollmentRequest;
import com.englishcenter.enrollment.dto.TransferEnrollmentRequest;
import com.englishcenter.enrollment.dto.TransferEnrollmentResponse;
import com.englishcenter.enrollment.dto.EnrollmentResponse;
import com.englishcenter.enrollment.dto.HoldEnrollmentRequest;
import com.englishcenter.enrollment.dto.ReactivateEnrollmentRequest;
import com.englishcenter.enrollment.mapper.EnrollmentMapper;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.invoice.InvoiceTestSupport;
import com.englishcenter.invoice.mapper.InvoiceMapper;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.student.Student;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.student.StudentStatus;
import com.englishcenter.student.mapper.StudentMapper;
import com.englishcenter.studentpackage.StudentPackage;
import com.englishcenter.studentpackage.StudentPackageRepository;
import com.englishcenter.studentpackage.StudentPackageStatus;
import com.englishcenter.studentpackage.mapper.StudentPackageMapper;
import com.englishcenter.tuitionpackage.TuitionPackage;
import com.englishcenter.tuitionpackage.TuitionPackageRepository;
import com.englishcenter.tuitionpackage.TuitionPackageStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {
    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private EnrollmentStatusHistoryRepository statusHistoryRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private TuitionPackageRepository tuitionPackageRepository;

    @Mock
    private ClassPackageRepository classPackageRepository;

    @Mock
    private StudentPackageRepository studentPackageRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private final EnrollmentMapper enrollmentMapper = new EnrollmentMapper(
            new StudentPackageMapper(),
            InvoiceTestSupport.invoiceMapper()
    );
    private final StudentMapper studentMapper = new StudentMapper();

    @Test
    void enrollStudentCreatesEnrollmentStudentPackageAndInvoice() {
        EnrollmentService service = newService();
        mockValidLookups(false, true);
        mockSaves();

        EnrollmentResponse response = service.enrollStudent(validRequest());

        assertThat(response.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(response.studentPackage()).isNotNull();
        assertThat(response.studentPackage().status()).isEqualTo(StudentPackageStatus.CONFIRMED);
        assertThat(response.invoice()).isNotNull();
        assertThat(response.invoice().status()).isEqualTo(InvoiceStatus.UNPAID);
        verify(enrollmentRepository).save(any(Enrollment.class));
        verify(statusHistoryRepository).save(any(EnrollmentStatusHistory.class));
        verify(studentPackageRepository).save(any(StudentPackage.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void enrollStudentRejectsDuplicateActiveEnrollment() {
        EnrollmentService service = newService();
        mockValidLookups(true, true);

        assertThatThrownBy(() -> service.enrollStudent(validRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Học viên đang học trong lớp này.");

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
        verify(studentPackageRepository, never()).save(any(StudentPackage.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void enrollStudentRejectsInactiveStudent() {
        EnrollmentService service = newService();
        Student student = student();
        student.setStatus(StudentStatus.INACTIVE);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(classroomRepository.findById(2L)).thenReturn(Optional.of(classroom()));
        when(tuitionPackageRepository.findById(3L)).thenReturn(Optional.of(tuitionPackage()));

        assertThatThrownBy(() -> service.enrollStudent(validRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Only active students can be enrolled");

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void enrollStudentRejectsCompletedClassroom() {
        EnrollmentService service = newService();
        Classroom classroom = classroom();
        classroom.setStatus(ClassroomStatus.COMPLETED);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student()));
        when(classroomRepository.findById(2L)).thenReturn(Optional.of(classroom));
        when(tuitionPackageRepository.findById(3L)).thenReturn(Optional.of(tuitionPackage()));

        assertThatThrownBy(() -> service.enrollStudent(validRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot enroll students into completed or canceled classroom");

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void enrollStudentRejectsPackageNotLinkedToClassroom() {
        EnrollmentService service = newService();
        mockValidLookups(false, false);

        assertThatThrownBy(() -> service.enrollStudent(validRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tuition package is not linked to this classroom");

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
        verify(studentPackageRepository, never()).save(any(StudentPackage.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void enrollStudentStoresPackageSnapshotInInvoice() {
        EnrollmentService service = newService();
        mockValidLookups(false, true);
        mockSaves();

        service.enrollStudent(validRequest());

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice invoice = invoiceCaptor.getValue();

        assertThat(invoice.getPackageNameSnapshot()).isEqualTo("8 sessions");
        assertThat(invoice.getTotalSessionsSnapshot()).isEqualTo(8);
        assertThat(invoice.getAmount()).isEqualByComparingTo("500000");
        assertThat(invoice.getDiscountAmount()).isEqualByComparingTo("50000");
        assertThat(invoice.getFinalAmount()).isEqualByComparingTo("450000");
    }

    @Test
    void enrollStudentCreatesUnpaidInvoiceWithZeroPaidAndRemainingFinalAmount() {
        EnrollmentService service = newService();
        mockValidLookups(false, true);
        mockSaves();

        service.enrollStudent(validRequest());

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice invoice = invoiceCaptor.getValue();

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        assertThat(invoice.getPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(invoice.getRemainingAmount()).isEqualByComparingTo(invoice.getFinalAmount());
    }

    @Test
    void enrollStudentRejectsInvalidLearningStartDate() {
        EnrollmentService service = newService();
        mockValidLookups(false, true);

        EnrollStudentRequest request = new EnrollStudentRequest(
                1L,
                2L,
                3L,
                LocalDate.of(2026, 6, 29),
                LocalDate.of(2026, 6, 29),
                new BigDecimal("50000"),
                "First enrollment"
        );

        assertThatThrownBy(() -> service.enrollStudent(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(EnrollmentLearningDateHelper.LEARNING_START_BEFORE_CLASSROOM_MESSAGE);

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void enrollStudentRejectsLearningStartDateNotMatchingStudyDays() {
        EnrollmentService service = newService();
        mockValidLookups(false, true);

        EnrollStudentRequest request = new EnrollStudentRequest(
                1L,
                2L,
                3L,
                LocalDate.of(2026, 7, 7),
                LocalDate.of(2026, 6, 29),
                new BigDecimal("50000"),
                "First enrollment"
        );

        assertThatThrownBy(() -> service.enrollStudent(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(EnrollmentLearningDateHelper.LEARNING_START_MUST_MATCH_DAYS_MESSAGE);

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void enrollStudentDefaultsLearningStartDateWhenNotProvided() {
        EnrollmentService service = newService();
        mockValidLookups(false, true);
        mockSaves();

        EnrollStudentRequest request = new EnrollStudentRequest(
                1L,
                2L,
                3L,
                null,
                LocalDate.of(2026, 6, 29),
                new BigDecimal("50000"),
                "First enrollment"
        );

        service.enrollStudent(request);

        ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(enrollmentCaptor.capture());
        assertThat(enrollmentCaptor.getValue().getStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    void stopClosesActiveEnrollmentWithoutChangingCounters() {
        EnrollmentService service = newService();
        Enrollment enrollment = activeEnrollment(8, 5);
        when(enrollmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        EnrollmentResponse response = service.stop(
                10L,
                new StopEnrollmentRequest(LocalDate.of(2026, 7, 20), "Không tiếp tục học")
        );

        assertThat(response.status()).isEqualTo(EnrollmentStatus.STOPPED);
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(response.totalSessions()).isEqualTo(8);
        assertThat(response.usedSessions()).isEqualTo(5);
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(studentPackageRepository, never()).save(any(StudentPackage.class));
    }

    @Test
    void holdCreatesClosedOnHoldPeriodWithoutFinancialRecords() {
        EnrollmentService service = newService();
        Enrollment enrollment = activeEnrollment(8, 2);
        when(enrollmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(enrollment));

        service.hold(10L, new HoldEnrollmentRequest(
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 8, 3),
                "Tạm nghỉ"
        ));

        ArgumentCaptor<EnrollmentStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(EnrollmentStatusHistory.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ON_HOLD);
        assertThat(historyCaptor.getValue().getStatus()).isEqualTo(EnrollmentStatus.ON_HOLD);
        assertThat(historyCaptor.getValue().getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(historyCaptor.getValue().getEffectiveTo()).isEqualTo(LocalDate.of(2026, 8, 3));
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(studentPackageRepository, never()).save(any(StudentPackage.class));
    }

    @Test
    void reactivateAdjustsLatestPeriodAndPreservesCounters() {
        EnrollmentService service = newService();
        Enrollment enrollment = activeEnrollment(8, 5);
        enrollment.setStatus(EnrollmentStatus.ON_HOLD);
        when(enrollmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(enrollment));
        EnrollmentStatusHistory onHoldHistory = new EnrollmentStatusHistory();
        onHoldHistory.setStatus(EnrollmentStatus.ON_HOLD);
        onHoldHistory.setEffectiveFrom(LocalDate.of(2026, 7, 20));
        onHoldHistory.setEffectiveTo(LocalDate.of(2026, 8, 3));
        when(statusHistoryRepository.latestForUpdate(10L)).thenReturn(Optional.of(onHoldHistory));
        when(enrollmentRepository.existsByStudentIdAndClassroomIdAndStatusAndIdNot(
                1L, 2L, EnrollmentStatus.ACTIVE, 10L
        )).thenReturn(false);
        when(classSessionRepository.findByClassroomIdOrderBySessionDateAscStartTimeAsc(2L))
                .thenReturn(List.of());

        EnrollmentResponse response = service.reactivate(
                10L,
                new ReactivateEnrollmentRequest(LocalDate.of(2026, 7, 22), "Quay lại học")
        );

        assertThat(response.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(response.totalSessions()).isEqualTo(8);
        assertThat(response.usedSessions()).isEqualTo(5);
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(studentPackageRepository, never()).save(any(StudentPackage.class));
    }

    @Test
    void reactivateResolvesNonStudyDateToNextValidSessionDate() {
        EnrollmentService service = newService();
        Enrollment enrollment = activeEnrollment(8, 5);
        enrollment.setStatus(EnrollmentStatus.ON_HOLD);
        enrollment.getClassroom().setStatus(ClassroomStatus.ONGOING);
        when(enrollmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(enrollment));
        EnrollmentStatusHistory onHoldHistory = new EnrollmentStatusHistory();
        onHoldHistory.setStatus(EnrollmentStatus.ON_HOLD);
        onHoldHistory.setEffectiveFrom(LocalDate.of(2026, 7, 20));
        when(statusHistoryRepository.latestForUpdate(10L)).thenReturn(Optional.of(onHoldHistory));
        when(enrollmentRepository.existsByStudentIdAndClassroomIdAndStatusAndIdNot(
                1L, 2L, EnrollmentStatus.ACTIVE, 10L
        )).thenReturn(false);
        when(classSessionRepository.findByClassroomIdOrderBySessionDateAscStartTimeAsc(2L))
                .thenReturn(List.of(
                        classSession(LocalDate.of(2026, 7, 20)),
                        classSession(LocalDate.of(2026, 7, 22)),
                        classSession(LocalDate.of(2026, 7, 27))
                ));

        // Requested Friday 24/07; next non-canceled session is Monday 27/07.
        EnrollmentResponse response = service.reactivate(
                10L,
                new ReactivateEnrollmentRequest(LocalDate.of(2026, 7, 24), "Xin học lại trước buổi tới")
        );

        assertThat(response.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        ArgumentCaptor<EnrollmentStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(EnrollmentStatusHistory.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(historyCaptor.getValue().getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 7, 27));
        assertThat(onHoldHistory.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 7, 27));
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(studentPackageRepository, never()).save(any(StudentPackage.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void stopThenReactivateBuildsHalfOpenHistoryIntervalsForAttendanceGaps() {
        EnrollmentService service = newService();
        Enrollment enrollment = activeEnrollment(8, 2);
        enrollment.getClassroom().setStatus(ClassroomStatus.ONGOING);

        EnrollmentStatusHistory activeHistory = new EnrollmentStatusHistory();
        activeHistory.setId(1L);
        activeHistory.setStatus(EnrollmentStatus.ACTIVE);
        activeHistory.setEffectiveFrom(LocalDate.of(2026, 7, 1));

        java.util.concurrent.atomic.AtomicReference<EnrollmentStatusHistory> latest =
                new java.util.concurrent.atomic.AtomicReference<>(activeHistory);
        java.util.concurrent.atomic.AtomicLong nextId =
                new java.util.concurrent.atomic.AtomicLong(2L);

        when(enrollmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(enrollment));
        when(statusHistoryRepository.latestForUpdate(10L))
                .thenAnswer(invocation -> Optional.of(latest.get()));
        when(statusHistoryRepository.save(any(EnrollmentStatusHistory.class))).thenAnswer(invocation -> {
            EnrollmentStatusHistory history = invocation.getArgument(0);
            if (history.getId() == null) {
                history.setId(nextId.getAndIncrement());
            }
            if (history.getEffectiveTo() == null) {
                latest.set(history);
            }
            return history;
        });
        when(enrollmentRepository.existsByStudentIdAndClassroomIdAndStatusAndIdNot(
                1L, 2L, EnrollmentStatus.ACTIVE, 10L
        )).thenReturn(false);
        when(classSessionRepository.findByClassroomIdOrderBySessionDateAscStartTimeAsc(2L))
                .thenReturn(List.of(
                        classSession(LocalDate.of(2026, 7, 1)),
                        classSession(LocalDate.of(2026, 7, 2)),
                        classSession(LocalDate.of(2026, 7, 9)),
                        classSession(LocalDate.of(2026, 7, 10)),
                        classSession(LocalDate.of(2026, 7, 15)),
                        classSession(LocalDate.of(2026, 7, 16))
                ));

        service.stop(10L, new StopEnrollmentRequest(LocalDate.of(2026, 7, 9), "Ngừng học"));
        assertThat(activeHistory.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 7, 9));
        assertThat(latest.get().getStatus()).isEqualTo(EnrollmentStatus.STOPPED);
        assertThat(latest.get().getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 7, 9));
        assertThat(latest.get().getEffectiveTo()).isNull();

        EnrollmentStatusHistory stoppedHistory = latest.get();
        service.reactivate(
                10L,
                new ReactivateEnrollmentRequest(LocalDate.of(2026, 7, 14), "Học lại")
        );

        assertThat(stoppedHistory.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(latest.get().getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(latest.get().getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(latest.get().getEffectiveTo()).isNull();
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(enrollment.getUsedSessions()).isEqualTo(2);
        assertThat(enrollment.getTotalSessions()).isEqualTo(8);
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(studentPackageRepository, never()).save(any(StudentPackage.class));
    }

    @Test
    void lifecycleAllowsTransitionAtCurrentPeriodStartBoundary() {
        EnrollmentService service = newService();
        Enrollment enrollment = activeEnrollment(8, 0);
        when(enrollmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(enrollment));

        EnrollmentResponse response = service.stop(
                10L,
                new StopEnrollmentRequest(LocalDate.of(2026, 7, 1), "Ngừng cùng ngày")
        );

        assertThat(response.status()).isEqualTo(EnrollmentStatus.STOPPED);
    }

    @Test
    void stopBeforeLearningStartCollapsesFutureActivePeriod() {
        EnrollmentService service = newService();
        Enrollment enrollment = activeEnrollment(8, 0);
        enrollment.setStartDate(LocalDate.of(2026, 8, 3));
        EnrollmentStatusHistory activeHistory = new EnrollmentStatusHistory();
        activeHistory.setStatus(EnrollmentStatus.ACTIVE);
        activeHistory.setEffectiveFrom(LocalDate.of(2026, 8, 3));
        when(enrollmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(enrollment));
        when(statusHistoryRepository.latestForUpdate(10L)).thenReturn(Optional.of(activeHistory));

        EnrollmentResponse response = service.stop(
                10L,
                new StopEnrollmentRequest(LocalDate.of(2026, 7, 20), "Hủy trước ngày vào học")
        );

        assertThat(response.status()).isEqualTo(EnrollmentStatus.STOPPED);
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(activeHistory.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 8, 3));
        ArgumentCaptor<EnrollmentStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(EnrollmentStatusHistory.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStatus()).isEqualTo(EnrollmentStatus.STOPPED);
        assertThat(historyCaptor.getValue().getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 7, 20));
    }

    @Test
    void cancelBeforeLearningStartIsAllowed() {
        EnrollmentService service = newService();
        Enrollment enrollment = activeEnrollment(8, 0);
        enrollment.setStartDate(LocalDate.of(2026, 8, 3));
        EnrollmentStatusHistory activeHistory = new EnrollmentStatusHistory();
        activeHistory.setStatus(EnrollmentStatus.ACTIVE);
        activeHistory.setEffectiveFrom(LocalDate.of(2026, 8, 3));
        when(enrollmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(enrollment));
        when(statusHistoryRepository.latestForUpdate(10L)).thenReturn(Optional.of(activeHistory));
        when(invoiceRepository.findAllByEnrollmentIdForUpdate(10L)).thenReturn(List.of());
        when(studentPackageRepository.findAllByEnrollmentId(10L)).thenReturn(List.of());

        CancelEnrollmentResponse response = service.cancel(
                10L,
                new CancelEnrollmentRequest(LocalDate.of(2026, 7, 27), "Đổi ý trước khi vào học")
        );

        assertThat(response.status()).isEqualTo(EnrollmentStatus.CANCELED);
        assertThat(response.canceledInvoiceCount()).isZero();
        assertThat(activeHistory.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 8, 3));
    }

    @Test
    void reactivateRejectsEffectiveDateBeforeCurrentPeriodStart() {
        EnrollmentService service = newService();
        Enrollment enrollment = activeEnrollment(8, 5);
        enrollment.setStatus(EnrollmentStatus.ON_HOLD);
        when(enrollmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(enrollment));
        EnrollmentStatusHistory onHoldHistory = new EnrollmentStatusHistory();
        onHoldHistory.setStatus(EnrollmentStatus.ON_HOLD);
        onHoldHistory.setEffectiveFrom(LocalDate.of(2026, 7, 20));
        onHoldHistory.setEffectiveTo(LocalDate.of(2026, 8, 3));
        when(statusHistoryRepository.latestForUpdate(10L)).thenReturn(Optional.of(onHoldHistory));
        when(enrollmentRepository.existsByStudentIdAndClassroomIdAndStatusAndIdNot(
                1L, 2L, EnrollmentStatus.ACTIVE, 10L
        )).thenReturn(false);
        when(classSessionRepository.findByClassroomIdOrderBySessionDateAscStartTimeAsc(2L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.reactivate(
                10L,
                new ReactivateEnrollmentRequest(LocalDate.of(2026, 7, 15), "Quay lại sớm hơn kỳ bảo lưu")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Effective date must not be before the current period start date");
    }

    @Test
    void transferCarriesOnlyRemainingSessionsAndCreatesNoFinancialRecords() {
        EnrollmentService service = newService();
        Enrollment source = activeEnrollment(8, 5);
        Classroom targetClassroom = classroom();
        targetClassroom.setId(4L);
        targetClassroom.setClassName("Starter B");
        when(enrollmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(source));
        when(classroomRepository.findById(4L)).thenReturn(Optional.of(targetClassroom));
        when(classSessionRepository.countByClassroomId(4L)).thenReturn(0);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(11L);
            }
            return saved;
        });

        TransferEnrollmentResponse response = service.transfer(
                10L,
                new TransferEnrollmentRequest(4L, LocalDate.of(2026, 7, 22), "Chuyển lịch học")
        );

        assertThat(response.sourceEnrollment().status()).isEqualTo(EnrollmentStatus.TRANSFERRED);
        assertThat(response.targetEnrollment().status()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(response.transferredSessions()).isEqualTo(3);
        assertThat(response.targetEnrollment().totalSessions()).isEqualTo(3);
        assertThat(source.getTotalSessions()).isEqualTo(8);
        assertThat(source.getUsedSessions()).isEqualTo(5);
        verify(studentPackageRepository, never()).save(any(StudentPackage.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void cancelRejectsAnyValidAttendanceInEnrollmentPeriod() {
        EnrollmentService service = newService();
        Enrollment enrollment = activeEnrollment(8, 0);
        when(enrollmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(enrollment));
        when(attendanceRepository.countForEnrollmentPeriod(
                1L, 2L, enrollment.getStartDate(), null
        )).thenReturn(1L);

        assertThatThrownBy(() -> service.cancel(
                10L,
                new CancelEnrollmentRequest(LocalDate.of(2026, 7, 27), "Tạo nhầm")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ghi danh đã có dữ liệu điểm danh. Vui lòng sử dụng chức năng Ngừng học.");
    }

    @Test
    void cancelSoftCancelsEnrollmentPackageAndCollectibleInvoice() {
        EnrollmentService service = newService();
        Enrollment enrollment = activeEnrollment(8, 0);
        StudentPackage studentPackage = new StudentPackage();
        studentPackage.setId(20L);
        studentPackage.setStudent(enrollment.getStudent());
        studentPackage.setClassroom(enrollment.getClassroom());
        studentPackage.setEnrollment(enrollment);
        studentPackage.setTuitionPackage(enrollment.getSelectedPackage());
        studentPackage.setStatus(StudentPackageStatus.CONFIRMED);
        studentPackage.setSourceType(com.englishcenter.studentpackage.StudentPackageSourceType.ENROLLMENT);
        Invoice invoice = new Invoice();
        invoice.setId(30L);
        invoice.setStudent(enrollment.getStudent());
        invoice.setClassroom(enrollment.getClassroom());
        invoice.setEnrollment(enrollment);
        invoice.setStudentPackage(studentPackage);
        invoice.setStatus(InvoiceStatus.UNPAID);
        when(enrollmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(enrollment));
        when(invoiceRepository.findAllByEnrollmentIdForUpdate(10L)).thenReturn(List.of(invoice));
        when(studentPackageRepository.findAllByEnrollmentId(10L)).thenReturn(List.of(studentPackage));

        CancelEnrollmentResponse response = service.cancel(
                10L,
                new CancelEnrollmentRequest(LocalDate.of(2026, 7, 27), "Tạo nhầm")
        );

        assertThat(response.status()).isEqualTo(EnrollmentStatus.CANCELED);
        assertThat(response.canceledInvoiceIds()).containsExactly(30L);
        assertThat(response.canceledInvoiceCount()).isEqualTo(1);
        assertThat(studentPackage.getStatus()).isEqualTo(StudentPackageStatus.CANCELED);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELED);
        assertThat(invoice.getCancelReason()).isEqualTo("Tạo nhầm");
        assertThat(invoice.getCanceledAt()).isNotNull();
    }

    @Test
    void cancelCancelsCollectibleInvoiceRegardlessOfPackageSourceType() {
        EnrollmentService service = newService();
        Enrollment enrollment = activeEnrollment(8, 0);
        StudentPackage renewalPackage = new StudentPackage();
        renewalPackage.setId(21L);
        renewalPackage.setStudent(enrollment.getStudent());
        renewalPackage.setClassroom(enrollment.getClassroom());
        renewalPackage.setEnrollment(enrollment);
        renewalPackage.setTuitionPackage(enrollment.getSelectedPackage());
        renewalPackage.setStatus(StudentPackageStatus.CONFIRMED);
        renewalPackage.setSourceType(com.englishcenter.studentpackage.StudentPackageSourceType.RENEWAL);
        Invoice renewalInvoice = new Invoice();
        renewalInvoice.setId(31L);
        renewalInvoice.setEnrollment(enrollment);
        renewalInvoice.setStudentPackage(renewalPackage);
        renewalInvoice.setStatus(InvoiceStatus.UNPAID);
        renewalInvoice.setRemainingAmount(new BigDecimal("500000"));
        when(enrollmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(enrollment));
        when(invoiceRepository.findAllByEnrollmentIdForUpdate(10L)).thenReturn(List.of(renewalInvoice));
        when(studentPackageRepository.findAllByEnrollmentId(10L)).thenReturn(List.of(renewalPackage));

        CancelEnrollmentResponse response = service.cancel(
                10L,
                new CancelEnrollmentRequest(LocalDate.of(2026, 7, 27), "Tạo nhầm")
        );

        assertThat(response.canceledInvoiceIds()).containsExactly(31L);
        assertThat(renewalInvoice.getStatus()).isEqualTo(InvoiceStatus.CANCELED);
    }

    @Test
    void cancelRejectsValidPaymentWithRequiredMessage() {
        EnrollmentService service = newService();
        Enrollment enrollment = activeEnrollment(8, 0);
        Invoice invoice = new Invoice();
        invoice.setId(30L);
        when(enrollmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(enrollment));
        when(invoiceRepository.findAllByEnrollmentIdForUpdate(10L)).thenReturn(List.of(invoice));
        when(paymentRepository.existsByInvoiceIdAndStatus(
                30L,
                com.englishcenter.payment.PaymentStatus.VALID
        )).thenReturn(true);

        assertThatThrownBy(() -> service.cancel(
                10L,
                new CancelEnrollmentRequest(LocalDate.of(2026, 7, 27), "Tạo nhầm")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ghi danh đã có thanh toán hợp lệ. Không thể hủy ghi danh.");

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
        verify(statusHistoryRepository, never()).save(any(EnrollmentStatusHistory.class));
    }

    @Test
    void cancelAlreadyCanceledIsIdempotent() {
        EnrollmentService service = newService();
        Enrollment enrollment = activeEnrollment(8, 0);
        enrollment.setStatus(EnrollmentStatus.CANCELED);
        when(enrollmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(enrollment));

        CancelEnrollmentResponse response = service.cancel(
                10L,
                new CancelEnrollmentRequest(LocalDate.of(2026, 7, 27), "Tạo nhầm lần 2")
        );

        assertThat(response.status()).isEqualTo(EnrollmentStatus.CANCELED);
        assertThat(response.canceledInvoiceCount()).isZero();
        verify(invoiceRepository, never()).findAllByEnrollmentIdForUpdate(anyLong());
        verify(statusHistoryRepository, never()).save(any(EnrollmentStatusHistory.class));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void stopDoesNotCancelInvoice() {
        EnrollmentService service = newService();
        Enrollment enrollment = activeEnrollment(8, 0);
        Invoice invoice = new Invoice();
        invoice.setId(30L);
        invoice.setStatus(InvoiceStatus.UNPAID);
        when(enrollmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(enrollment));

        EnrollmentResponse response = service.stop(
                10L,
                new StopEnrollmentRequest(LocalDate.of(2026, 7, 20), "Nghỉ học")
        );

        assertThat(response.status()).isEqualTo(EnrollmentStatus.STOPPED);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        verify(invoiceRepository, never()).saveAll(any());
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(invoiceRepository, never()).findAllByEnrollmentIdForUpdate(anyLong());
    }

    @Test
    void holdDoesNotCancelInvoice() {
        EnrollmentService service = newService();
        Enrollment enrollment = activeEnrollment(8, 0);
        Invoice invoice = new Invoice();
        invoice.setId(30L);
        invoice.setStatus(InvoiceStatus.UNPAID);
        when(enrollmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(enrollment));

        EnrollmentResponse response = service.hold(
                10L,
                new HoldEnrollmentRequest(LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 20), "Bảo lưu")
        );

        assertThat(response.status()).isEqualTo(EnrollmentStatus.ON_HOLD);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        verify(invoiceRepository, never()).saveAll(any());
        verify(invoiceRepository, never()).findAllByEnrollmentIdForUpdate(anyLong());
    }

    @Test
    void duplicateDiagnosticsAggregateGroupWithoutMutation() {
        EnrollmentService service = newService();
        Enrollment first = activeEnrollment(8, 0);
        Enrollment second = activeEnrollment(8, 0);
        second.setId(11L);
        second.setStatus(EnrollmentStatus.CANCELED);
        second.setStartDate(LocalDate.of(2026, 7, 15));
        second.setEndDate(LocalDate.of(2026, 7, 16));
        when(enrollmentRepository.findDuplicateStudentClassroomEnrollments())
                .thenReturn(List.of(first, second));
        when(attendanceRepository.countForEnrollmentPeriod(
                1L, 2L, LocalDate.of(2026, 7, 1), null
        )).thenReturn(2L);
        when(invoiceRepository.countByEnrollmentIdIn(List.of(10L, 11L))).thenReturn(2L);
        when(paymentRepository.countValidByEnrollmentIds(List.of(10L, 11L))).thenReturn(1L);

        var diagnostics = service.getDuplicates();

        assertThat(diagnostics).hasSize(1);
        assertThat(diagnostics.getFirst().enrollmentIds()).containsExactly(10L, 11L);
        assertThat(diagnostics.getFirst().attendanceCount()).isEqualTo(2);
        assertThat(diagnostics.getFirst().invoiceCount()).isEqualTo(2);
        assertThat(diagnostics.getFirst().validPaymentCount()).isEqualTo(1);
        assertThat(diagnostics.getFirst().requiresManualCleanup()).isTrue();
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    private EnrollmentService newService() {
        lenient().when(statusHistoryRepository.latestForUpdate(anyLong())).thenAnswer(invocation -> {
            EnrollmentStatusHistory history = new EnrollmentStatusHistory();
            history.setEffectiveFrom(LocalDate.of(2026, 7, 1));
            history.setStatus(EnrollmentStatus.ACTIVE);
            return Optional.of(history);
        });
        lenient().when(statusHistoryRepository.saveAndFlush(any(EnrollmentStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(statusHistoryRepository.save(any(EnrollmentStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        return new EnrollmentService(
                enrollmentRepository,
                statusHistoryRepository,
                studentRepository,
                classroomRepository,
                tuitionPackageRepository,
                classPackageRepository,
                studentPackageRepository,
                invoiceRepository,
                classSessionRepository,
                attendanceRepository,
                paymentRepository,
                enrollmentMapper,
                studentMapper,
                InvoiceTestSupport.billingSnapshotService(),
                new AppTimeProperties()
        );
    }

    private void mockValidLookups(boolean hasDuplicateEnrollment, boolean packageLinkedToClassroom) {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student()));
        when(classroomRepository.findById(2L)).thenReturn(Optional.of(classroom()));
        when(tuitionPackageRepository.findById(3L)).thenReturn(Optional.of(tuitionPackage()));
        when(classPackageRepository.existsByClassroomIdAndTuitionPackageIdAndActiveTrue(2L, 3L))
                .thenReturn(packageLinkedToClassroom);

        if (packageLinkedToClassroom) {
            when(enrollmentRepository.findFirstByStudentIdAndClassroomIdAndStatusInOrderByIdDesc(
                    1L,
                    2L,
                    List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.ON_HOLD, EnrollmentStatus.STOPPED)
            )).thenReturn(hasDuplicateEnrollment
                    ? Optional.of(activeEnrollment(8, 0))
                    : Optional.empty());
            lenient().when(classSessionRepository.countByClassroomId(2L)).thenReturn(0);
        }
    }

    private void mockSaves() {
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            enrollment.setId(10L);
            enrollment.setCreatedAt(LocalDateTime.now());
            enrollment.setUpdatedAt(LocalDateTime.now());
            return enrollment;
        });
        when(studentPackageRepository.save(any(StudentPackage.class))).thenAnswer(invocation -> {
            StudentPackage studentPackage = invocation.getArgument(0);
            studentPackage.setId(20L);
            studentPackage.setCreatedAt(LocalDateTime.now());
            studentPackage.setUpdatedAt(LocalDateTime.now());
            return studentPackage;
        });
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            invoice.setId(30L);
            invoice.setCreatedAt(LocalDateTime.now());
            invoice.setUpdatedAt(LocalDateTime.now());
            return invoice;
        });
        when(statusHistoryRepository.save(any(EnrollmentStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private EnrollStudentRequest validRequest() {
        return new EnrollStudentRequest(
                1L,
                2L,
                3L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 6, 29),
                new BigDecimal("50000"),
                "First enrollment"
        );
    }

    private Student student() {
        Student student = new Student();
        student.setId(1L);
        student.setStudentCode("STU001");
        student.setFullName("Nguyen Van A");
        student.setStatus(StudentStatus.ACTIVE);
        return student;
    }

    private Classroom classroom() {
        Classroom classroom = new Classroom();
        classroom.setId(2L);
        classroom.setClassCode("CLS001");
        classroom.setClassName("Starter A");
        classroom.setStatus(ClassroomStatus.PLANNED);
        classroom.setStartDate(LocalDate.of(2026, 7, 1));
        classroom.setDaysOfWeek(Set.of(ClassDayOfWeek.MONDAY, ClassDayOfWeek.WEDNESDAY));
        return classroom;
    }

    private ClassSession classSession(LocalDate sessionDate) {
        ClassSession session = new ClassSession();
        session.setSessionDate(sessionDate);
        session.setStatus(ClassSessionStatus.SCHEDULED);
        return session;
    }

    private TuitionPackage tuitionPackage() {
        TuitionPackage tuitionPackage = new TuitionPackage();
        tuitionPackage.setId(3L);
        tuitionPackage.setName("8 sessions");
        tuitionPackage.setTotalSessions(8);
        tuitionPackage.setPrice(new BigDecimal("500000"));
        tuitionPackage.setStatus(TuitionPackageStatus.ACTIVE);
        return tuitionPackage;
    }

    private Enrollment activeEnrollment(int totalSessions, int usedSessions) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(10L);
        enrollment.setStudent(student());
        enrollment.setClassroom(classroom());
        enrollment.setStartDate(LocalDate.of(2026, 7, 1));
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setSelectedPackage(tuitionPackage());
        enrollment.setPackageNameSnapshot("8 sessions");
        enrollment.setTotalSessions(totalSessions);
        enrollment.setUsedSessions(usedSessions);
        enrollment.setTotalSessionsSnapshot(8);
        enrollment.setPackagePriceSnapshot(new BigDecimal("500000"));
        enrollment.setDiscountAmount(BigDecimal.ZERO);
        enrollment.setFinalAmount(new BigDecimal("500000"));
        return enrollment;
    }
}
