package com.englishcenter.dashboard;

import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classroom.ClassroomStatus;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.dashboard.dto.DashboardSummaryResponse;
import com.englishcenter.dashboard.dto.DashboardTodaySessionResponse;
import com.englishcenter.dashboard.dto.SessionWarningResponse;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.makeupcredit.MakeupCreditRepository;
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.payment.PaymentStatus;
import com.englishcenter.payment.dto.PaymentResponse;
import com.englishcenter.payment.mapper.PaymentMapper;
import com.englishcenter.report.dto.DebtReportItemResponse;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.student.StudentStatus;
import com.englishcenter.studentpackage.LearningProgressWarningType;
import com.englishcenter.attendance.AttendanceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private static final int DEFAULT_LOW_SESSION_THRESHOLD = 2;

    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final MakeupCreditRepository makeupCreditRepository;
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRepository attendanceRepository;

    public DashboardService(
            StudentRepository studentRepository,
            ClassroomRepository classroomRepository,
            EnrollmentRepository enrollmentRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            PaymentMapper paymentMapper,
            MakeupCreditRepository makeupCreditRepository,
            ClassSessionRepository classSessionRepository,
            AttendanceRepository attendanceRepository
    ) {
        this.studentRepository = studentRepository;
        this.classroomRepository = classroomRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.makeupCreditRepository = makeupCreditRepository;
        this.classSessionRepository = classSessionRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate firstDayOfYear = today.withDayOfYear(1);

        return new DashboardSummaryResponse(
                studentRepository.countByStatus(StudentStatus.ACTIVE),
                classroomRepository.countByStatus(ClassroomStatus.ONGOING),
                enrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE),
                enrollmentRepository.countDepletedByStatus(EnrollmentStatus.ACTIVE),
                enrollmentRepository.countLowSessionsByStatus(EnrollmentStatus.ACTIVE, DEFAULT_LOW_SESSION_THRESHOLD),
                invoiceRepository.countDistinctStudentsWithDebt(),
                invoiceRepository.countByStatus(InvoiceStatus.UNPAID),
                invoiceRepository.countByStatus(InvoiceStatus.PARTIALLY_PAID),
                invoiceRepository.sumDebtAmount(),
                paymentRepository.sumValidAmountBetween(today, today),
                paymentRepository.sumValidAmountBetween(firstDayOfMonth, today),
                paymentRepository.sumValidAmountBetween(firstDayOfYear, today),
                makeupCreditRepository.sumAllAvailableMakeupSessions(MakeupCreditStatus.AVAILABLE),
                classSessionRepository.countBySessionDateAndStatusNot(today, ClassSessionStatus.CANCELED),
                classSessionRepository.countBySessionDateBetweenAndStatus(
                        firstDayOfMonth,
                        today,
                        ClassSessionStatus.COMPLETED
                )
        );
    }

    @Transactional(readOnly = true)
    public List<DashboardTodaySessionResponse> getTodaySessions() {
        return classSessionRepository.findBySessionDateOrderByStartTimeAsc(LocalDate.now())
                .stream()
                .map(this::toTodaySession)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DebtReportItemResponse> getDebtAlerts(int limit) {
        int size = limit > 0 ? Math.min(limit, 20) : 10;
        return invoiceRepository.findDebtInvoices(
                        PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .map(this::toDebtAlert)
                .getContent();
    }

    @Transactional(readOnly = true)
    public List<SessionWarningResponse> getSessionWarnings(int remainingThreshold) {
        int threshold = remainingThreshold > 0 ? remainingThreshold : DEFAULT_LOW_SESSION_THRESHOLD;

        return enrollmentRepository.findSessionWarnings(threshold)
                .stream()
                .map(this::toSessionWarning)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getRecentPayments(int limit) {
        int size = limit > 0 ? Math.min(limit, 20) : 10;
        return paymentRepository.findRecentValidPayments(
                        PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "paymentDate", "createdAt"))
                )
                .map(paymentMapper::toResponse)
                .getContent();
    }

    private DashboardTodaySessionResponse toTodaySession(ClassSession session) {
        DashboardSessionAttendanceStatus attendanceStatus = resolveAttendanceStatus(session);
        long activeStudentCount = enrollmentRepository.countByClassroomIdAndStatus(
                session.getClassroom().getId(),
                EnrollmentStatus.ACTIVE
        );

        return new DashboardTodaySessionResponse(
                session.getId(),
                session.getClassroom().getId(),
                session.getClassroom().getClassName(),
                session.getClassroom().getTeacherName(),
                session.getClassroom().getRoom(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                (int) activeStudentCount,
                attendanceStatus
        );
    }

    private DashboardSessionAttendanceStatus resolveAttendanceStatus(ClassSession session) {
        if (session.getStatus() == ClassSessionStatus.CANCELED) {
            return DashboardSessionAttendanceStatus.CANCELED;
        }

        if (session.getStatus() == ClassSessionStatus.COMPLETED
                || attendanceRepository.existsBySessionId(session.getId())) {
            return DashboardSessionAttendanceStatus.MARKED;
        }

        return DashboardSessionAttendanceStatus.NOT_MARKED;
    }

    private DebtReportItemResponse toDebtAlert(Invoice invoice) {
        return new DebtReportItemResponse(
                invoice.getStudent().getId(),
                invoice.getStudent().getFullName(),
                invoice.getClassroom().getClassName(),
                invoice.getId(),
                invoice.getInvoiceCode(),
                invoice.getStudentPackage().getSourceType(),
                invoice.getCreatedAt(),
                invoice.getDueDate(),
                invoice.getFinalAmount(),
                invoice.getPaidAmount(),
                invoice.getRemainingAmount(),
                invoice.getStatus(),
                invoice.getPackageNameSnapshot()
        );
    }

    private SessionWarningResponse toSessionWarning(Enrollment enrollment) {
        int totalSessions = enrollment.getTotalSessions();
        int usedSessions = enrollment.getUsedSessions();
        int remainingSessions = Math.max(totalSessions - usedSessions, 0);
        int overusedSessions = Math.max(usedSessions - totalSessions, 0);
        LearningProgressWarningType warningType = resolveWarningType(remainingSessions, overusedSessions);
        String warningMessage = buildWarningMessage(remainingSessions, overusedSessions);

        return new SessionWarningResponse(
                enrollment.getStudent().getId(),
                enrollment.getStudent().getFullName(),
                enrollment.getClassroom().getId(),
                enrollment.getClassroom().getClassName(),
                enrollment.getId(),
                totalSessions,
                usedSessions,
                remainingSessions,
                warningType,
                warningMessage
        );
    }

    private LearningProgressWarningType resolveWarningType(int remainingSessions, int overusedSessions) {
        if (overusedSessions > 0 || remainingSessions <= 0) {
            return LearningProgressWarningType.DEPLETED;
        }

        return LearningProgressWarningType.LOW;
    }

    private String buildWarningMessage(int remainingSessions, int overusedSessions) {
        if (overusedSessions > 0 || remainingSessions <= 0) {
            return "Hết buổi - cần gia hạn";
        }

        return "Sắp hết buổi";
    }
}
