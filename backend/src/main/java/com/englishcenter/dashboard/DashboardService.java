package com.englishcenter.dashboard;

import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.auth.AccountRole;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classroom.ClassroomStatus;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.dashboard.dto.DashboardAlertResponse;
import com.englishcenter.dashboard.dto.DashboardOverviewResponse;
import com.englishcenter.dashboard.dto.DashboardOverviewSummaryResponse;
import com.englishcenter.dashboard.dto.DashboardPendingAttendanceResponse;
import com.englishcenter.dashboard.dto.DashboardSummaryResponse;
import com.englishcenter.dashboard.dto.DashboardTodaySessionResponse;
import com.englishcenter.dashboard.dto.SessionWarningResponse;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentSessionService;
import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.invoice.mapper.InvoiceMapper;
import com.englishcenter.makeupcredit.MakeupCreditRepository;
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.payment.dto.PaymentResponse;
import com.englishcenter.payment.mapper.PaymentMapper;
import com.englishcenter.report.dto.DebtReportItemResponse;
import com.englishcenter.security.CurrentUserService;
import com.englishcenter.security.SecurityUtils;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.student.StudentStatus;
import com.englishcenter.studentpackage.LearningProgressWarningType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private static final int DEFAULT_LOW_SESSION_THRESHOLD = DashboardAlertService.DEFAULT_LOW_SESSION_THRESHOLD;
    private static final int WORK_SECTION_LIMIT = 10;

    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentSessionService enrollmentSessionService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final MakeupCreditRepository makeupCreditRepository;
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final DashboardAlertService dashboardAlertService;
    private final CurrentUserService currentUserService;

    public DashboardService(
            StudentRepository studentRepository,
            ClassroomRepository classroomRepository,
            EnrollmentRepository enrollmentRepository,
            EnrollmentSessionService enrollmentSessionService,
            InvoiceRepository invoiceRepository,
            InvoiceMapper invoiceMapper,
            PaymentRepository paymentRepository,
            PaymentMapper paymentMapper,
            MakeupCreditRepository makeupCreditRepository,
            ClassSessionRepository classSessionRepository,
            AttendanceRepository attendanceRepository,
            DashboardAlertService dashboardAlertService,
            CurrentUserService currentUserService
    ) {
        this.studentRepository = studentRepository;
        this.classroomRepository = classroomRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentSessionService = enrollmentSessionService;
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.makeupCreditRepository = makeupCreditRepository;
        this.classSessionRepository = classSessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.dashboardAlertService = dashboardAlertService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview() {
        AccountRole role = SecurityUtils.requirePrincipal().role();
        List<DashboardAlertResponse> alerts = dashboardAlertService.getAlertsForCurrentUser();

        return switch (role) {
            case ADMIN -> buildAdminOverview(alerts);
            case TEACHER -> buildTeacherOverview(alerts);
            case STUDENT -> buildStudentOverview(alerts);
        };
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
                makeupCreditRepository.countAllAvailableMakeupCredits(MakeupCreditStatus.AVAILABLE),
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
        return getSessionWarnings(remainingThreshold, null);
    }

    @Transactional(readOnly = true)
    public List<SessionWarningResponse> getSessionWarnings(int remainingThreshold, String remainingFilter) {
        int threshold = remainingThreshold > 0 ? remainingThreshold : DEFAULT_LOW_SESSION_THRESHOLD;
        String filter = remainingFilter == null ? "" : remainingFilter.trim().toUpperCase();

        List<Enrollment> enrollments;
        if (filter.isEmpty()) {
            // Legacy combined warnings (depleted + low)
            enrollments = enrollmentRepository.findSessionWarnings(threshold);
        } else {
            enrollments = switch (filter) {
                case "ZERO" -> enrollmentRepository.findDepletedActiveEnrollments();
                case "LOW" -> enrollmentRepository.findLowSessionActiveEnrollments(threshold);
                case "AVAILABLE" -> enrollmentRepository.findAvailableSessionActiveEnrollments(threshold);
                default -> List.of(); // invalid values are ignored safely
            };
        }

        return enrollments.stream().map(this::toSessionWarning).toList();
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

    private DashboardOverviewResponse buildAdminOverview(List<DashboardAlertResponse> alerts) {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        DashboardOverviewSummaryResponse summary = new DashboardOverviewSummaryResponse(
                studentRepository.countByStatus(StudentStatus.ACTIVE),
                classroomRepository.countByStatus(ClassroomStatus.ONGOING),
                (long) classSessionRepository.countBySessionDateAndStatusNot(today, ClassSessionStatus.CANCELED),
                paymentRepository.sumValidAmountBetween(firstDayOfMonth, today),
                invoiceRepository.sumDebtAmount(),
                invoiceRepository.countDistinctStudentsWithDebt(),
                enrollmentRepository.countDepletedByStatus(EnrollmentStatus.ACTIVE),
                enrollmentRepository.countLowSessionsByStatus(EnrollmentStatus.ACTIVE, DEFAULT_LOW_SESSION_THRESHOLD),
                null,
                classSessionRepository.countIncompleteAttendance(today),
                null,
                null
        );

        List<DashboardTodaySessionResponse> todaySessions = getTodaySessions();
        List<DashboardPendingAttendanceResponse> pendingAttendance = classSessionRepository
                .findIncompleteAttendance(today)
                .stream()
                .limit(WORK_SECTION_LIMIT)
                .map(this::toPendingAttendance)
                .toList();
        List<SessionWarningResponse> needingRenewal = enrollmentRepository
                .findDepletedActiveEnrollments()
                .stream()
                .limit(WORK_SECTION_LIMIT)
                .map(this::toSessionWarning)
                .toList();
        List<DebtReportItemResponse> overdueInvoices = invoiceRepository
                .findOverdueInvoices(today, PageRequest.of(0, WORK_SECTION_LIMIT))
                .map(this::toDebtAlert)
                .getContent();

        return new DashboardOverviewResponse(
                AccountRole.ADMIN,
                summary,
                alerts,
                todaySessions,
                pendingAttendance,
                needingRenewal,
                overdueInvoices,
                List.of()
        );
    }

    private DashboardOverviewResponse buildTeacherOverview(List<DashboardAlertResponse> alerts) {
        Long teacherId = currentUserService.requireCurrentTeacherId();
        LocalDate today = LocalDate.now();

        long assignedClassrooms = classroomRepository.countByTeacherId(teacherId);
        long activeStudents = enrollmentRepository.findByClassroomTeacherId(teacherId).stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
                .map(enrollment -> enrollment.getStudent().getId())
                .distinct()
                .count();
        long todayClasses = classSessionRepository.countTodayByTeacherId(teacherId, today);
        long incomplete = classSessionRepository.countIncompleteAttendanceByTeacherId(teacherId, today);
        long outOfSessions = enrollmentRepository.countDepletedByTeacherIdAndStatus(
                teacherId,
                EnrollmentStatus.ACTIVE
        );
        long lowSessions = enrollmentRepository.countLowSessionsByTeacherIdAndStatus(
                teacherId,
                EnrollmentStatus.ACTIVE,
                DEFAULT_LOW_SESSION_THRESHOLD
        );

        DashboardOverviewSummaryResponse summary = new DashboardOverviewSummaryResponse(
                activeStudents,
                assignedClassrooms,
                todayClasses,
                null,
                null,
                null,
                outOfSessions,
                lowSessions,
                assignedClassrooms,
                incomplete,
                null,
                null
        );

        List<DashboardTodaySessionResponse> todaySessions = classSessionRepository
                .findTodayByTeacherId(teacherId, today)
                .stream()
                .map(this::toTodaySession)
                .toList();
        List<DashboardPendingAttendanceResponse> pendingAttendance = classSessionRepository
                .findIncompleteAttendanceByTeacherId(teacherId, today)
                .stream()
                .limit(WORK_SECTION_LIMIT)
                .map(this::toPendingAttendance)
                .toList();
        List<SessionWarningResponse> needingRenewal = enrollmentRepository.findByClassroomTeacherId(teacherId).stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
                .filter(enrollment -> enrollmentSessionService.remainingSessions(enrollment) <= DEFAULT_LOW_SESSION_THRESHOLD)
                .sorted((left, right) -> Integer.compare(
                        enrollmentSessionService.remainingSessions(left),
                        enrollmentSessionService.remainingSessions(right)
                ))
                .limit(WORK_SECTION_LIMIT)
                .map(this::toSessionWarning)
                .toList();

        return new DashboardOverviewResponse(
                AccountRole.TEACHER,
                summary,
                alerts,
                todaySessions,
                pendingAttendance,
                needingRenewal,
                List.of(),
                List.of()
        );
    }

    private DashboardOverviewResponse buildStudentOverview(List<DashboardAlertResponse> alerts) {
        Long studentId = currentUserService.requireCurrentStudentId();
        LocalDate today = LocalDate.now();

        List<Enrollment> activeEnrollments = enrollmentRepository.findByStudentIdAndStatus(
                studentId,
                EnrollmentStatus.ACTIVE
        );
        int remainingSessions = activeEnrollments.stream()
                .mapToInt(enrollmentSessionService::remainingSessions)
                .sum();
        BigDecimal outstanding = invoiceRepository.findDebtInvoicesByStudentId(studentId).stream()
                .map(Invoice::getRemainingAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long todayClasses = classSessionRepository.findByEnrolledStudentId(studentId).stream()
                .filter(session -> session.getSessionDate().equals(today))
                .filter(session -> session.getStatus() != ClassSessionStatus.CANCELED)
                .count();

        DashboardOverviewSummaryResponse summary = new DashboardOverviewSummaryResponse(
                null,
                (long) activeEnrollments.size(),
                todayClasses,
                null,
                null,
                null,
                activeEnrollments.stream()
                        .filter(enrollment -> enrollmentSessionService.remainingSessions(enrollment) <= 0)
                        .count(),
                activeEnrollments.stream()
                        .mapToInt(enrollmentSessionService::remainingSessions)
                        .filter(remaining -> remaining > 0 && remaining <= DEFAULT_LOW_SESSION_THRESHOLD)
                        .count(),
                null,
                null,
                remainingSessions,
                outstanding
        );

        List<DashboardTodaySessionResponse> todaySessions = classSessionRepository.findByEnrolledStudentId(studentId)
                .stream()
                .filter(session -> session.getSessionDate().equals(today))
                .filter(session -> session.getStatus() != ClassSessionStatus.CANCELED)
                .map(this::toTodaySession)
                .toList();

        List<InvoiceResponse> studentInvoices = invoiceRepository.findDebtInvoicesByStudentId(studentId).stream()
                .map(invoiceMapper::toResponse)
                .toList();

        List<SessionWarningResponse> needingRenewal = activeEnrollments.stream()
                .filter(enrollment -> enrollmentSessionService.remainingSessions(enrollment) <= DEFAULT_LOW_SESSION_THRESHOLD)
                .map(this::toSessionWarning)
                .toList();

        return new DashboardOverviewResponse(
                AccountRole.STUDENT,
                summary,
                alerts,
                todaySessions,
                List.of(),
                needingRenewal,
                List.of(),
                studentInvoices
        );
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

    private DashboardPendingAttendanceResponse toPendingAttendance(ClassSession session) {
        int eligible = enrollmentRepository.findEligibleForAttendanceBySessionDate(
                session.getClassroom().getId(),
                session.getSessionDate()
        ).size();
        int marked = (int) attendanceRepository.countBySessionIdAndValidTrue(session.getId());
        int missing = Math.max(eligible - marked, 0);

        return new DashboardPendingAttendanceResponse(
                session.getId(),
                session.getClassroom().getId(),
                session.getClassroom().getClassName(),
                session.getClassroom().getTeacherName(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                eligible,
                marked,
                missing
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
                invoice.getStudent().getStudentCode(),
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
                enrollment.getStudent().getStudentCode(),
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
        if (remainingSessions <= DEFAULT_LOW_SESSION_THRESHOLD) {
            return LearningProgressWarningType.LOW;
        }
        return LearningProgressWarningType.OK;
    }

    private String buildWarningMessage(int remainingSessions, int overusedSessions) {
        if (overusedSessions > 0 || remainingSessions <= 0) {
            return "Hết buổi - cần gia hạn";
        }
        if (remainingSessions <= DEFAULT_LOW_SESSION_THRESHOLD) {
            return "Sắp hết buổi";
        }
        return "Còn buổi";
    }
}
