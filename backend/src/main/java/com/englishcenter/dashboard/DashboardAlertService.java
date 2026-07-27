package com.englishcenter.dashboard;

import com.englishcenter.academic.assessment.AssessmentRepository;
import com.englishcenter.academic.assessment.AssessmentStatus;
import com.englishcenter.academic.assignment.AssignmentRepository;
import com.englishcenter.academic.assignment.AssignmentStatus;
import com.englishcenter.academic.evaluation.EvaluationPeriodRepository;
import com.englishcenter.academic.evaluation.StudentEvaluationRepository;
import com.englishcenter.academic.evaluation.StudentEvaluationStatus;
import com.englishcenter.academic.report.ProgressReportStatus;
import com.englishcenter.academic.report.StudentProgressReportRepository;
import com.englishcenter.academic.score.AssessmentScoreRepository;
import com.englishcenter.academic.submission.AssignmentSubmissionRepository;
import com.englishcenter.academic.submission.SubmissionStatus;
import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classroom.ClassroomStatus;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.common.config.AppTimeProperties;
import com.englishcenter.dashboard.dto.DashboardAlertResponse;
import com.englishcenter.enrollment.Enrollment;
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
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.security.CurrentUserService;
import com.englishcenter.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardAlertService {
    static final int DEFAULT_LOW_SESSION_THRESHOLD = 2;

    private static final Comparator<DashboardAlertResponse> ALERT_ORDER = Comparator
            .comparingInt((DashboardAlertResponse alert) -> severityRank(alert.severity()))
            .thenComparingInt(DashboardAlertResponse::priority)
            .reversed()
            .thenComparing(Comparator.comparingLong(DashboardAlertResponse::count).reversed());

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentSessionService enrollmentSessionService;
    private final InvoiceRepository invoiceRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassSessionRepository classSessionRepository;
    private final FinanceCalculationService financeCalculationService;
    private final FinancialPeriodService financialPeriodService;
    private final CurrentUserService currentUserService;
    private final AssessmentRepository assessmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final EvaluationPeriodRepository evaluationPeriodRepository;
    private final StudentEvaluationRepository studentEvaluationRepository;
    private final AssessmentScoreRepository assessmentScoreRepository;
    private final StudentProgressReportRepository studentProgressReportRepository;
    private final AppTimeProperties appTimeProperties;

    public DashboardAlertService(
            EnrollmentRepository enrollmentRepository,
            EnrollmentSessionService enrollmentSessionService,
            InvoiceRepository invoiceRepository,
            ClassroomRepository classroomRepository,
            ClassSessionRepository classSessionRepository,
            FinanceCalculationService financeCalculationService,
            FinancialPeriodService financialPeriodService,
            CurrentUserService currentUserService,
            AssessmentRepository assessmentRepository,
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository assignmentSubmissionRepository,
            EvaluationPeriodRepository evaluationPeriodRepository,
            StudentEvaluationRepository studentEvaluationRepository,
            AssessmentScoreRepository assessmentScoreRepository,
            StudentProgressReportRepository studentProgressReportRepository,
            AppTimeProperties appTimeProperties
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentSessionService = enrollmentSessionService;
        this.invoiceRepository = invoiceRepository;
        this.classroomRepository = classroomRepository;
        this.classSessionRepository = classSessionRepository;
        this.financeCalculationService = financeCalculationService;
        this.financialPeriodService = financialPeriodService;
        this.currentUserService = currentUserService;
        this.assessmentRepository = assessmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
        this.evaluationPeriodRepository = evaluationPeriodRepository;
        this.studentEvaluationRepository = studentEvaluationRepository;
        this.assessmentScoreRepository = assessmentScoreRepository;
        this.studentProgressReportRepository = studentProgressReportRepository;
        this.appTimeProperties = appTimeProperties;
    }

    @Transactional(readOnly = true)
    public List<DashboardAlertResponse> getAlertsForCurrentUser() {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        return switch (principal.role()) {
            case ADMIN -> getAdminAlerts();
            case TEACHER -> getTeacherAlerts(currentUserService.requireCurrentTeacherId());
            case STUDENT -> getStudentAlerts(currentUserService.requireCurrentStudentId());
        };
    }

    @Transactional(readOnly = true)
    public List<DashboardAlertResponse> getAdminAlerts() {
        LocalDate today = LocalDate.now();
        int threshold = DEFAULT_LOW_SESSION_THRESHOLD;
        List<DashboardAlertResponse> alerts = new ArrayList<>();

        long outOfSessions = enrollmentRepository.countDepletedByStatus(EnrollmentStatus.ACTIVE);
        addIfPositive(alerts, alert(
                DashboardAlertType.STUDENT_OUT_OF_SESSIONS,
                DashboardAlertSeverity.CRITICAL,
                outOfSessions + " học viên đã hết buổi",
                "Các học viên đang học nhưng không còn buổi.",
                outOfSessions,
                "Xem học viên hết buổi",
                "/students?remaining=ZERO",
                100
        ));

        long lowSessions = enrollmentRepository.countLowSessionsByStatus(EnrollmentStatus.ACTIVE, threshold);
        addIfPositive(alerts, alert(
                DashboardAlertType.STUDENT_LOW_SESSIONS,
                DashboardAlertSeverity.WARNING,
                lowSessions + " học viên còn 1–" + threshold + " buổi",
                "Các học viên sắp hết buổi cần theo dõi gia hạn.",
                lowSessions,
                "Xem học viên sắp hết buổi",
                "/students?remaining=LOW",
                90
        ));

        long pendingAttendance = classSessionRepository.countIncompleteAttendance(today);
        addIfPositive(alerts, alert(
                DashboardAlertType.SESSION_ATTENDANCE_NOT_COMPLETED,
                DashboardAlertSeverity.CRITICAL,
                pendingAttendance + " buổi học chưa hoàn tất điểm danh",
                "Các buổi đã qua nhưng chưa có điểm danh hợp lệ.",
                pendingAttendance,
                "Xem buổi chưa điểm danh",
                "/dashboard#pending-attendance",
                95
        ));

        long overdueInvoices = invoiceRepository.countOverdueInvoices(today);
        addIfPositive(alerts, alert(
                DashboardAlertType.OVERDUE_INVOICES,
                DashboardAlertSeverity.CRITICAL,
                overdueInvoices + " hóa đơn đã quá hạn",
                "Hóa đơn còn nợ và đã quá hạn thanh toán.",
                overdueInvoices,
                "Xem hóa đơn quá hạn",
                "/debts?status=OVERDUE",
                98
        ));

        long multiUnpaid = invoiceRepository.countStudentsWithMultipleUnpaidInvoices();
        addIfPositive(alerts, alert(
                DashboardAlertType.STUDENTS_WITH_MULTIPLE_UNPAID_INVOICES,
                DashboardAlertSeverity.WARNING,
                multiUnpaid + " học viên có nhiều hóa đơn còn nợ",
                "Học viên có từ 2 hóa đơn chưa thanh toán đủ trở lên.",
                multiUnpaid,
                "Xem học viên nợ nhiều hóa đơn",
                "/debts?status=MULTIPLE_UNPAID",
                80
        ));

        long unassigned = classroomRepository.countByTeacherIdIsNullAndStatus(ClassroomStatus.ONGOING);
        addIfPositive(alerts, alert(
                DashboardAlertType.UNASSIGNED_CLASSROOM,
                DashboardAlertSeverity.WARNING,
                unassigned + " lớp chưa được phân công giáo viên",
                "Lớp đang hoạt động nhưng chưa có giáo viên phụ trách.",
                unassigned,
                "Xem lớp chưa phân công",
                "/classrooms?teacherAssignment=UNASSIGNED",
                85
        ));

        long negativeAccounts = financeCalculationService.calculateAccountBalances(today).stream()
                .filter(AccountBalanceResponse::negativeBalance)
                .count();
        addIfPositive(alerts, alert(
                DashboardAlertType.NEGATIVE_FINANCIAL_ACCOUNT,
                DashboardAlertSeverity.CRITICAL,
                negativeAccounts + " tài khoản tài chính đang âm",
                "Số dư tài khoản tính toán đang nhỏ hơn 0.",
                negativeAccounts,
                "Xem tài khoản đang âm",
                "/finance?tab=accounts&balance=NEGATIVE",
                97
        ));

        PaymentReconciliationResponse reconciliation = financeCalculationService.reconcilePayments(null, null);
        if (reconciliation.status() == ReconciliationStatus.MISMATCHED) {
            long mismatchCount = reconciliation.mismatches() == null || reconciliation.mismatches().isEmpty()
                    ? 1L
                    : reconciliation.mismatches().size();
            addIfPositive(alerts, alert(
                    DashboardAlertType.PAYMENT_RECONCILIATION_MISMATCH,
                    DashboardAlertSeverity.CRITICAL,
                    mismatchCount + " đối soát thanh toán không khớp",
                    "Đối soát thanh toán đang ở trạng thái MISMATCHED.",
                    mismatchCount,
                    "Xem đối soát lỗi",
                    "/finance?tab=reconciliation&reconStatus=MISMATCHED",
                    96
            ));
        }

        YearMonth previousMonth = YearMonth.from(today).minusMonths(1);
        FinancialPeriodResponse previousPeriod = financialPeriodService.getPeriod(
                previousMonth.getYear(),
                previousMonth.getMonthValue()
        );
        if (previousPeriod.status() == FinancialPeriodStatus.OPEN) {
            addIfPositive(alerts, alert(
                    DashboardAlertType.PREVIOUS_MONTH_NOT_CLOSED,
                    DashboardAlertSeverity.WARNING,
                    "Tháng trước chưa khóa sổ",
                    "Tháng " + String.format("%02d/%d", previousMonth.getMonthValue(), previousMonth.getYear())
                            + " vẫn đang mở.",
                    1,
                    "Xem khóa sổ tháng trước",
                    "/finance?tab=periods",
                    70
            ));
        }

        long assessmentsWaiting = assessmentRepository.countByStatus(AssessmentStatus.OPEN);
        addIfPositive(alerts, alert(
                DashboardAlertType.ASSESSMENTS_WAITING_SCORES,
                DashboardAlertSeverity.WARNING,
                assessmentsWaiting + " bài kiểm tra đang mở chờ điểm",
                "Các bài kiểm tra còn ở trạng thái OPEN cần hoàn tất nhập điểm.",
                assessmentsWaiting,
                "Xem bài kiểm tra",
                "/academic/assessments?status=OPEN",
                75
        ));

        long missingSubmissions = assignmentRepository.countPublishedWithoutSubmissions(AssignmentStatus.PUBLISHED);
        addIfPositive(alerts, alert(
                DashboardAlertType.PUBLISHED_ASSIGNMENTS_MISSING_SUBMISSIONS,
                DashboardAlertSeverity.WARNING,
                missingSubmissions + " bài tập đã xuất bản chưa có bài nộp",
                "Bài tập đang mở nhưng chưa nhận được bài nộp nào.",
                missingSubmissions,
                "Xem bài tập",
                "/academic/assignments?status=PUBLISHED",
                72
        ));

        LocalDate businessToday = LocalDate.now(appTimeProperties.zoneId());
        long periodsNearingEnd = evaluationPeriodRepository.countOpenEndingBetween(
                businessToday,
                businessToday.plusDays(7)
        );
        addIfPositive(alerts, alert(
                DashboardAlertType.EVALUATION_PERIODS_NEARING_END_INCOMPLETE,
                DashboardAlertSeverity.WARNING,
                periodsNearingEnd + " kỳ đánh giá sắp kết thúc",
                "Kỳ đánh giá OPEN sẽ kết thúc trong 7 ngày tới.",
                periodsNearingEnd,
                "Xem kỳ đánh giá",
                "/academic/evaluation-periods?status=OPEN",
                68
        ));

        return sortAlerts(alerts);
    }

    @Transactional(readOnly = true)
    public List<DashboardAlertResponse> getTeacherAlerts(Long teacherId) {
        LocalDate today = LocalDate.now();
        int threshold = DEFAULT_LOW_SESSION_THRESHOLD;
        List<DashboardAlertResponse> alerts = new ArrayList<>();

        long todayClasses = classSessionRepository.countTodayByTeacherId(teacherId, today);
        addIfPositive(alerts, alert(
                DashboardAlertType.TODAY_ASSIGNED_CLASSES,
                DashboardAlertSeverity.INFO,
                todayClasses + " buổi học hôm nay",
                "Các buổi học trong lớp bạn phụ trách hôm nay.",
                todayClasses,
                "Xem buổi học hôm nay",
                "/me/sessions?filter=TODAY",
                40
        ));

        long pendingAttendance = classSessionRepository.countIncompleteAttendanceByTeacherId(teacherId, today);
        addIfPositive(alerts, alert(
                DashboardAlertType.TEACHER_SESSION_ATTENDANCE_NOT_COMPLETED,
                DashboardAlertSeverity.CRITICAL,
                pendingAttendance + " buổi chưa hoàn tất điểm danh",
                "Buổi đã qua trong lớp bạn phụ trách chưa có điểm danh hợp lệ.",
                pendingAttendance,
                "Xem buổi chưa điểm danh",
                "/me/attendance?status=PENDING",
                100
        ));

        long outOfSessions = enrollmentRepository.countDepletedByTeacherIdAndStatus(
                teacherId,
                EnrollmentStatus.ACTIVE
        );
        addIfPositive(alerts, alert(
                DashboardAlertType.TEACHER_STUDENTS_OUT_OF_SESSIONS,
                DashboardAlertSeverity.WARNING,
                outOfSessions + " học viên đã hết buổi",
                "Học viên trong lớp bạn phụ trách đã hết buổi (chỉ xem tiến độ).",
                outOfSessions,
                "Xem học viên hết buổi",
                "/me/progress?remaining=ZERO",
                90
        ));

        long lowSessions = enrollmentRepository.countLowSessionsByTeacherIdAndStatus(
                teacherId,
                EnrollmentStatus.ACTIVE,
                threshold
        );
        addIfPositive(alerts, alert(
                DashboardAlertType.TEACHER_STUDENTS_LOW_SESSIONS,
                DashboardAlertSeverity.WARNING,
                lowSessions + " học viên còn 1–" + threshold + " buổi",
                "Học viên trong lớp bạn phụ trách sắp hết buổi.",
                lowSessions,
                "Xem học viên sắp hết buổi",
                "/me/progress?remaining=LOW",
                85
        ));

        long upcoming = classSessionRepository.countUpcomingByTeacherId(teacherId, today);
        addIfPositive(alerts, alert(
                DashboardAlertType.UPCOMING_SESSIONS,
                DashboardAlertSeverity.INFO,
                upcoming + " buổi học sắp tới",
                "Các buổi học tiếp theo trong lớp bạn phụ trách.",
                upcoming,
                "Xem buổi học sắp tới",
                "/me/sessions?filter=UPCOMING",
                30
        ));

        LocalDate businessToday = LocalDate.now(appTimeProperties.zoneId());
        long ungraded = assignmentSubmissionRepository.countByTeacherIdAndStatusIn(
                teacherId,
                List.of(SubmissionStatus.SUBMITTED, SubmissionStatus.LATE)
        );
        addIfPositive(alerts, alert(
                DashboardAlertType.ASSIGNMENTS_WITH_UNGRADED_SUBMISSIONS,
                DashboardAlertSeverity.WARNING,
                ungraded + " bài nộp chưa chấm",
                "Học viên đã nộp bài nhưng chưa được chấm điểm.",
                ungraded,
                "Xem bài tập",
                "/academic/assignments",
                80
        ));

        long openAssessments = assessmentRepository.countByTeacherIdAndStatus(teacherId, AssessmentStatus.OPEN);
        addIfPositive(alerts, alert(
                DashboardAlertType.ASSESSMENTS_WITH_MISSING_SCORES,
                DashboardAlertSeverity.WARNING,
                openAssessments + " bài kiểm tra đang mở",
                "Các bài kiểm tra OPEN trong lớp bạn phụ trách cần nhập điểm.",
                openAssessments,
                "Xem bài kiểm tra",
                "/academic/assessments?status=OPEN",
                78
        ));

        long draftEvals = studentEvaluationRepository.countDraftNearPeriodEndByTeacher(
                teacherId,
                StudentEvaluationStatus.DRAFT,
                businessToday,
                businessToday.plusDays(7)
        );
        addIfPositive(alerts, alert(
                DashboardAlertType.DRAFT_EVALUATIONS_NEAR_PERIOD_END,
                DashboardAlertSeverity.WARNING,
                draftEvals + " nhận xét nháp gần hết kỳ",
                "Nhận xét DRAFT thuộc kỳ đánh giá sẽ kết thúc trong 7 ngày.",
                draftEvals,
                "Xem nhận xét",
                "/academic/evaluations",
                76
        ));

        return sortAlerts(alerts);
    }

    @Transactional(readOnly = true)
    public List<DashboardAlertResponse> getStudentAlerts(Long studentId) {
        LocalDate today = LocalDate.now();
        int threshold = DEFAULT_LOW_SESSION_THRESHOLD;
        List<DashboardAlertResponse> alerts = new ArrayList<>();

        List<Enrollment> activeEnrollments = enrollmentRepository.findByStudentIdAndStatus(
                studentId,
                EnrollmentStatus.ACTIVE
        );

        ClassSession nextSession = classSessionRepository.findByEnrolledStudentId(studentId).stream()
                .filter(session -> !session.getSessionDate().isBefore(today))
                .filter(session -> session.getStatus() != com.englishcenter.classsession.ClassSessionStatus.CANCELED)
                .min(Comparator
                        .comparing(ClassSession::getSessionDate)
                        .thenComparing(ClassSession::getStartTime))
                .orElse(null);
        if (nextSession != null) {
            addIfPositive(alerts, alert(
                    DashboardAlertType.NEXT_SESSION,
                    DashboardAlertSeverity.INFO,
                    "Buổi học tiếp theo: " + nextSession.getClassroom().getClassName(),
                    "Ngày " + nextSession.getSessionDate()
                            + " lúc " + nextSession.getStartTime().toString().substring(0, 5) + ".",
                    1,
                    "Xem lịch học",
                    "/student/learning?tab=schedule",
                    50
            ));
        }

        long outOfSessions = activeEnrollments.stream()
                .filter(enrollment -> enrollmentSessionService.remainingSessions(enrollment) <= 0)
                .count();
        addIfPositive(alerts, alert(
                DashboardAlertType.OUT_OF_SESSIONS,
                DashboardAlertSeverity.CRITICAL,
                outOfSessions + " lớp đã hết buổi",
                "Bạn không còn buổi học trong các lớp đang học.",
                outOfSessions,
                "Xem tiến độ",
                "/student/learning?tab=progress",
                100
        ));

        long lowSessions = activeEnrollments.stream()
                .mapToInt(enrollmentSessionService::remainingSessions)
                .filter(remaining -> remaining > 0 && remaining <= threshold)
                .count();
        addIfPositive(alerts, alert(
                DashboardAlertType.LOW_REMAINING_SESSIONS,
                DashboardAlertSeverity.WARNING,
                lowSessions + " lớp còn 1–" + threshold + " buổi",
                "Bạn sắp hết buổi học. Vui lòng liên hệ trung tâm để gia hạn.",
                lowSessions,
                "Xem tiến độ",
                "/student/learning?tab=progress",
                90
        ));

        BigDecimal outstanding = invoiceRepository.findDebtInvoicesByStudentId(studentId).stream()
                .map(Invoice::getRemainingAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
            addIfPositive(alerts, alert(
                    DashboardAlertType.OUTSTANDING_TUITION,
                    DashboardAlertSeverity.WARNING,
                    "Bạn còn học phí cần thanh toán",
                    "Tổng còn nợ: " + outstanding.toPlainString() + " VND.",
                    1,
                    "Xem công nợ",
                    "/student/tuition?tab=debt",
                    85
            ));
        }

        long overdue = invoiceRepository.countOverdueInvoicesByStudentId(studentId, today);
        addIfPositive(alerts, alert(
                DashboardAlertType.OVERDUE_INVOICE,
                DashboardAlertSeverity.CRITICAL,
                overdue + " hóa đơn đã quá hạn",
                "Hóa đơn của bạn đã quá hạn thanh toán.",
                overdue,
                "Xem hóa đơn",
                "/student/tuition?tab=invoices",
                95
        ));

        LocalDate businessToday = LocalDate.now(appTimeProperties.zoneId());
        long dueSoon = assignmentRepository.countDueSoonWithoutSubmissionForStudent(
                studentId,
                businessToday,
                businessToday.plusDays(2)
        );
        addIfPositive(alerts, alert(
                DashboardAlertType.ASSIGNMENT_DUE_SOON,
                DashboardAlertSeverity.WARNING,
                dueSoon + " bài tập sắp đến hạn",
                "Bài tập đến hạn trong 2 ngày tới và bạn chưa nộp.",
                dueSoon,
                "Xem bài tập",
                "/student/learning?tab=assignments",
                82
        ));

        long overdueAssignments = assignmentRepository.countOverdueWithoutSubmissionForStudent(
                studentId,
                businessToday
        );
        addIfPositive(alerts, alert(
                DashboardAlertType.OVERDUE_ASSIGNMENT,
                DashboardAlertSeverity.CRITICAL,
                overdueAssignments + " bài tập quá hạn",
                "Bài tập đã quá hạn và bạn chưa nộp.",
                overdueAssignments,
                "Xem bài tập",
                "/student/learning?tab=assignments",
                92
        ));

        LocalDateTime since = businessToday.minusDays(7).atStartOfDay();
        long newScores = assessmentScoreRepository.countPublishedSinceForStudent(studentId, since);
        addIfPositive(alerts, alert(
                DashboardAlertType.NEW_PUBLISHED_SCORE,
                DashboardAlertSeverity.INFO,
                newScores + " điểm mới được công bố",
                "Điểm bài kiểm tra mới trong 7 ngày gần đây.",
                newScores,
                "Xem kết quả",
                "/student/learning?tab=scores",
                45
        ));

        long newReports = studentProgressReportRepository.countPublishedSinceForStudent(
                studentId,
                List.of(ProgressReportStatus.PUBLISHED, ProgressReportStatus.FINALIZED),
                since
        );
        addIfPositive(alerts, alert(
                DashboardAlertType.NEW_PUBLISHED_PROGRESS_REPORT,
                DashboardAlertSeverity.INFO,
                newReports + " phiếu tổng kết mới",
                "Phiếu tổng kết học tập mới trong 7 ngày gần đây.",
                newReports,
                "Xem tổng kết",
                "/student/learning?tab=reports",
                44
        ));

        return sortAlerts(alerts);
    }

    public AccountRole currentRole() {
        return SecurityUtils.requirePrincipal().role();
    }

    private static DashboardAlertResponse alert(
            DashboardAlertType type,
            DashboardAlertSeverity severity,
            String title,
            String description,
            long count,
            String actionLabel,
            String actionUrl,
            int priority
    ) {
        return new DashboardAlertResponse(
                type,
                severity,
                title,
                description,
                count,
                actionLabel,
                actionUrl,
                priority
        );
    }

    private static void addIfPositive(List<DashboardAlertResponse> alerts, DashboardAlertResponse alert) {
        if (alert.count() > 0) {
            alerts.add(alert);
        }
    }

    private static List<DashboardAlertResponse> sortAlerts(List<DashboardAlertResponse> alerts) {
        return alerts.stream().sorted(ALERT_ORDER).toList();
    }

    private static int severityRank(DashboardAlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 3;
            case WARNING -> 2;
            case INFO -> 1;
        };
    }
}
