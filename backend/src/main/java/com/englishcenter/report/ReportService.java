package com.englishcenter.report;

import com.englishcenter.attendance.Attendance;
import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.attendance.AttendanceStatus;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.enrollment.EnrollmentProgressService;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.enrollment.dto.EnrollmentLearningProgressResponse;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.invoice.mapper.InvoiceMapper;
import com.englishcenter.payment.Payment;
import com.englishcenter.payment.PaymentMethod;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.payment.PaymentStatus;
import com.englishcenter.payment.dto.PaymentResponse;
import com.englishcenter.payment.mapper.PaymentMapper;
import com.englishcenter.report.dto.AttendanceReportItemResponse;
import com.englishcenter.report.dto.AttendanceReportResponse;
import com.englishcenter.report.dto.AttendanceReportSummary;
import com.englishcenter.report.dto.ClassroomReportItemResponse;
import com.englishcenter.report.dto.DebtReportItemResponse;
import com.englishcenter.report.dto.DebtReportSummary;
import com.englishcenter.report.dto.PaymentReportResponse;
import com.englishcenter.report.dto.RevenueByDateItem;
import com.englishcenter.report.dto.RevenueByPaymentMethodItem;
import com.englishcenter.report.dto.RevenueReportResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private static final int DEFAULT_LOW_SESSION_THRESHOLD = 2;
    private static final int MAX_PAGE_SIZE = 500;

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final EnrollmentProgressService enrollmentProgressService;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRepository attendanceRepository;

    public ReportService(
            InvoiceRepository invoiceRepository,
            InvoiceMapper invoiceMapper,
            PaymentRepository paymentRepository,
            PaymentMapper paymentMapper,
            EnrollmentProgressService enrollmentProgressService,
            EnrollmentRepository enrollmentRepository,
            ClassroomRepository classroomRepository,
            ClassSessionRepository classSessionRepository,
            AttendanceRepository attendanceRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.enrollmentProgressService = enrollmentProgressService;
        this.enrollmentRepository = enrollmentRepository;
        this.classroomRepository = classroomRepository;
        this.classSessionRepository = classSessionRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @Transactional(readOnly = true)
    public DebtReportSummary getDebtReportSummary() {
        return new DebtReportSummary(
                invoiceRepository.sumDebtAmount(),
                invoiceRepository.countDistinctStudentsWithDebt(),
                invoiceRepository.countByStatus(InvoiceStatus.UNPAID),
                invoiceRepository.countByStatus(InvoiceStatus.PARTIALLY_PAID)
        );
    }

    @Transactional(readOnly = true)
    public Page<DebtReportItemResponse> getDebtReport(
            Long classroomId,
            String keyword,
            InvoiceStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return invoiceRepository.searchDebtReport(
                        status,
                        classroomId,
                        normalizeKeyword(keyword),
                        fromDate,
                        toDate,
                        pageable
                )
                .map(this::toDebtReportItem);
    }

    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(
            LocalDate fromDate,
            LocalDate toDate,
            Long classroomId,
            PaymentMethod paymentMethod,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "paymentDate", "createdAt")
        );

        Page<Payment> payments = paymentRepository.searchValidPayments(
                fromDate,
                toDate,
                classroomId,
                paymentMethod,
                pageable
        );

        BigDecimal totalRevenue = paymentRepository.sumValidAmountFiltered(
                fromDate,
                toDate,
                classroomId,
                paymentMethod,
                null
        );

        List<RevenueByDateItem> revenueByDate = paymentRepository.sumValidAmountGroupByPaymentDate(
                        fromDate,
                        toDate,
                        classroomId,
                        paymentMethod
                )
                .stream()
                .map(row -> new RevenueByDateItem((LocalDate) row[0], (BigDecimal) row[1]))
                .toList();

        List<RevenueByPaymentMethodItem> revenueByPaymentMethod = paymentRepository.sumValidAmountGroupByMethod(
                        fromDate,
                        toDate,
                        classroomId,
                        paymentMethod
                )
                .stream()
                .map(row -> new RevenueByPaymentMethodItem((PaymentMethod) row[0], (BigDecimal) row[1]))
                .toList();

        List<PaymentResponse> paymentItems = payments.getContent()
                .stream()
                .map(paymentMapper::toResponse)
                .toList();

        return new RevenueReportResponse(
                totalRevenue,
                revenueByDate,
                revenueByPaymentMethod,
                paymentItems
        );
    }

    @Transactional(readOnly = true)
    public PaymentReportResponse getPaymentReport(
            LocalDate fromDate,
            LocalDate toDate,
            Long classroomId,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus,
            String keyword,
            int page,
            int size
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "paymentDate", "createdAt")
        );

        Page<Payment> payments = paymentRepository.searchPayments(
                paymentStatus,
                fromDate,
                toDate,
                classroomId,
                paymentMethod,
                normalizedKeyword,
                pageable
        );

        BigDecimal totalRevenue = paymentRepository.sumValidAmountFiltered(
                fromDate,
                toDate,
                classroomId,
                paymentMethod,
                normalizedKeyword
        );
        if (paymentStatus != null && paymentStatus != PaymentStatus.VALID) {
            totalRevenue = BigDecimal.ZERO;
        }

        long paymentCount = paymentRepository.countValidPaymentsFiltered(
                fromDate,
                toDate,
                classroomId,
                paymentMethod,
                normalizedKeyword
        );

        BigDecimal cashAmount = paymentRepository.sumValidAmountByMethodFiltered(
                PaymentMethod.CASH,
                fromDate,
                toDate,
                classroomId,
                normalizedKeyword
        );
        BigDecimal bankTransferAmount = paymentRepository.sumValidAmountByMethodFiltered(
                PaymentMethod.BANK_TRANSFER,
                fromDate,
                toDate,
                classroomId,
                normalizedKeyword
        );

        List<RevenueByDateItem> revenueByDate = paymentRepository.sumValidAmountGroupByPaymentDate(
                        fromDate,
                        toDate,
                        classroomId,
                        paymentMethod
                )
                .stream()
                .map(row -> new RevenueByDateItem((LocalDate) row[0], (BigDecimal) row[1]))
                .toList();

        List<PaymentResponse> paymentItems = payments.getContent()
                .stream()
                .map(paymentMapper::toResponse)
                .toList();

        return new PaymentReportResponse(
                totalRevenue,
                paymentCount,
                cashAmount,
                bankTransferAmount,
                revenueByDate,
                paymentItems
        );
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getInvoiceReport(
            Long classroomId,
            String keyword,
            InvoiceStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return invoiceRepository.searchInvoiceReport(
                        status,
                        classroomId,
                        normalizeKeyword(keyword),
                        fromDate,
                        toDate,
                        pageable
                )
                .map(invoiceMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AttendanceReportResponse getAttendanceReport(
            Long classroomId,
            String keyword,
            LocalDate sessionDate,
            AttendanceStatus status,
            int page,
            int size
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "markedAt")
        );

        Page<Attendance> attendancePage = attendanceRepository.searchReport(
                classroomId,
                normalizedKeyword,
                sessionDate,
                status,
                pageable
        );

        long totalCount = attendanceRepository.countReportFiltered(
                classroomId,
                normalizedKeyword,
                sessionDate,
                status
        );
        long presentCount = attendanceRepository.countReportByStatus(
                AttendanceStatus.PRESENT,
                classroomId,
                normalizedKeyword,
                sessionDate
        );
        long absentCount = attendanceRepository.countReportByStatus(
                AttendanceStatus.ABSENT,
                classroomId,
                normalizedKeyword,
                sessionDate
        );
        long excusedCount = attendanceRepository.countReportByStatus(
                AttendanceStatus.EXCUSED,
                classroomId,
                normalizedKeyword,
                sessionDate
        );
        double presentRate = totalCount == 0 ? 0 : (double) presentCount / totalCount * 100;

        List<AttendanceReportItemResponse> items = attendancePage.getContent()
                .stream()
                .map(this::toAttendanceReportItem)
                .toList();

        return new AttendanceReportResponse(
                new AttendanceReportSummary(totalCount, presentCount, absentCount, excusedCount, presentRate),
                items
        );
    }

    @Transactional(readOnly = true)
    public List<EnrollmentLearningProgressResponse> getEnrollmentProgressReport() {
        return enrollmentRepository.findAllActiveWithRelations()
                .stream()
                .map(enrollment -> enrollmentProgressService.getByEnrollmentId(enrollment.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClassroomReportItemResponse> getClassroomReport() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        return classroomRepository.findAll()
                .stream()
                .map(classroom -> toClassroomReportItem(classroom, today, firstDayOfMonth))
                .toList();
    }

    private ClassroomReportItemResponse toClassroomReportItem(
            Classroom classroom,
            LocalDate today,
            LocalDate firstDayOfMonth
    ) {
        Long classroomId = classroom.getId();

        return new ClassroomReportItemResponse(
                classroomId,
                classroom.getClassName(),
                classroom.getStatus(),
                enrollmentRepository.countByClassroomIdAndStatus(classroomId, EnrollmentStatus.ACTIVE),
                enrollmentRepository.countDepletedByClassroomIdAndStatus(classroomId, EnrollmentStatus.ACTIVE),
                enrollmentRepository.countLowSessionsByClassroomIdAndStatus(
                        classroomId,
                        EnrollmentStatus.ACTIVE,
                        DEFAULT_LOW_SESSION_THRESHOLD
                ),
                invoiceRepository.sumDebtAmountByClassroomId(classroomId),
                paymentRepository.sumValidAmountByClassroomBetween(classroomId, firstDayOfMonth, today),
                classSessionRepository.countByClassroomIdAndSessionDateGreaterThanEqualAndStatusNot(
                        classroomId,
                        today,
                        ClassSessionStatus.CANCELED
                )
        );
    }

    private DebtReportItemResponse toDebtReportItem(Invoice invoice) {
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

    private AttendanceReportItemResponse toAttendanceReportItem(Attendance attendance) {
        return new AttendanceReportItemResponse(
                attendance.getId(),
                attendance.getSession().getSessionDate(),
                attendance.getSession().getClassroom().getClassName(),
                attendance.getStudent().getStudentCode(),
                attendance.getStudent().getFullName(),
                attendance.getStatus(),
                attendance.getNote()
        );
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
