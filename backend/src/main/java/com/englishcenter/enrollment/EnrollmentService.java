package com.englishcenter.enrollment;

import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.classpackage.ClassPackageRepository;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classroom.ClassroomStatus;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.enrollment.dto.EnrollStudentRequest;
import com.englishcenter.enrollment.dto.CancelEnrollmentRequest;
import com.englishcenter.enrollment.dto.EnrollmentResponse;
import com.englishcenter.enrollment.dto.DuplicateEnrollmentGroupResponse;
import com.englishcenter.enrollment.dto.EnrollmentStatusHistoryResponse;
import com.englishcenter.enrollment.dto.HoldEnrollmentRequest;
import com.englishcenter.enrollment.dto.ReactivateEnrollmentRequest;
import com.englishcenter.enrollment.dto.StopEnrollmentRequest;
import com.englishcenter.enrollment.dto.TransferEnrollmentRequest;
import com.englishcenter.enrollment.dto.TransferEnrollmentResponse;
import com.englishcenter.enrollment.mapper.EnrollmentMapper;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.payment.PaymentStatus;
import com.englishcenter.student.Student;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.student.StudentStatus;
import com.englishcenter.student.dto.StudentResponse;
import com.englishcenter.student.mapper.StudentMapper;
import com.englishcenter.studentpackage.StudentPackage;
import com.englishcenter.studentpackage.StudentPackageRepository;
import com.englishcenter.studentpackage.StudentPackageSourceType;
import com.englishcenter.studentpackage.StudentPackageStatus;
import com.englishcenter.tuitionpackage.TuitionPackage;
import com.englishcenter.tuitionpackage.TuitionPackageRepository;
import com.englishcenter.tuitionpackage.TuitionPackageStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentStatusHistoryRepository statusHistoryRepository;
    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final TuitionPackageRepository tuitionPackageRepository;
    private final ClassPackageRepository classPackageRepository;
    private final StudentPackageRepository studentPackageRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final PaymentRepository paymentRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final com.englishcenter.invoice.InvoiceBillingSnapshotService invoiceBillingSnapshotService;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            EnrollmentStatusHistoryRepository statusHistoryRepository,
            StudentRepository studentRepository,
            ClassroomRepository classroomRepository,
            TuitionPackageRepository tuitionPackageRepository,
            ClassPackageRepository classPackageRepository,
            StudentPackageRepository studentPackageRepository,
            InvoiceRepository invoiceRepository,
            ClassSessionRepository classSessionRepository,
            AttendanceRepository attendanceRepository,
            PaymentRepository paymentRepository,
            EnrollmentMapper enrollmentMapper,
            StudentMapper studentMapper,
            com.englishcenter.invoice.InvoiceBillingSnapshotService invoiceBillingSnapshotService
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.studentRepository = studentRepository;
        this.classroomRepository = classroomRepository;
        this.tuitionPackageRepository = tuitionPackageRepository;
        this.classPackageRepository = classPackageRepository;
        this.studentPackageRepository = studentPackageRepository;
        this.invoiceRepository = invoiceRepository;
        this.classSessionRepository = classSessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.paymentRepository = paymentRepository;
        this.enrollmentMapper = enrollmentMapper;
        this.studentMapper = studentMapper;
        this.invoiceBillingSnapshotService = invoiceBillingSnapshotService;
    }

    @Transactional
    public EnrollmentResponse enrollStudent(EnrollStudentRequest request) {
        Student student = findStudent(request.studentId());
        Classroom classroom = findClassroom(request.classroomId());
        TuitionPackage tuitionPackage = findTuitionPackage(request.tuitionPackageId());
        BigDecimal discountAmount = normalizeDiscountAmount(request.discountAmount());

        validateStudentEligible(student);
        validateClassroomOpenForEnrollment(classroom);
        validateTuitionPackageActive(tuitionPackage);
        validateTuitionPackageBelongsToClassroom(classroom.getId(), tuitionPackage.getId());
        validateNoExistingEnrollment(student.getId(), classroom.getId());
        validateFinalAmount(tuitionPackage.getPrice(), discountAmount);

        LocalDate learningStartDate = resolveLearningStartDate(classroom, request.learningStartDate());
        LocalDate enrollmentDate = request.enrollmentDate() != null
                ? request.enrollmentDate()
                : LocalDate.now();

        BigDecimal adjustmentAmount = ZERO;
        BigDecimal finalAmount = tuitionPackage.getPrice().subtract(discountAmount).add(adjustmentAmount);

        Enrollment enrollment = createEnrollment(
                request,
                student,
                classroom,
                tuitionPackage,
                discountAmount,
                finalAmount,
                learningStartDate,
                EnrollmentCreationSource.ENROLLMENT
        );
        enrollment = enrollmentRepository.save(enrollment);
        createHistory(enrollment, EnrollmentStatus.ACTIVE, learningStartDate, null, "Ghi danh ban đầu");

        StudentPackage studentPackage = createStudentPackage(
                enrollment,
                student,
                classroom,
                tuitionPackage,
                discountAmount,
                adjustmentAmount,
                finalAmount,
                StudentPackageSourceType.ENROLLMENT
        );
        studentPackage = studentPackageRepository.save(studentPackage);

        Invoice invoice = createInvoice(
                enrollment,
                studentPackage,
                student,
                classroom,
                tuitionPackage,
                discountAmount,
                adjustmentAmount,
                finalAmount,
                enrollmentDate
        );
        invoice = invoiceRepository.save(invoice);

        // TODO: Save ActivityLog for ENROLL_STUDENT when the ActivityLog module exists.
        return enrollmentMapper.toResponse(enrollment, studentPackage, invoice);
    }

    /**
     * Shared enrollment creation for legacy Excel import.
     * Creates Enrollment + ACTIVE history + first StudentPackage + first UNPAID Invoice.
     * Does not create Payment, ClassSession, or Attendance.
     * Idempotent: returns existing enrollment when student+classroom already enrolled.
     */
    @Transactional
    public Enrollment enrollFromLegacyImport(
            Student student,
            Classroom classroom,
            TuitionPackage tuitionPackage,
            LocalDate learningStartDate
    ) {
        validateStudentEligible(student);
        validateClassroomOpenForEnrollment(classroom);
        validateTuitionPackageActive(tuitionPackage);
        validateTuitionPackageBelongsToClassroom(classroom.getId(), tuitionPackage.getId());

        List<Enrollment> existing = enrollmentRepository
                .findByStudentIdAndClassroomIdOrderByStartDateAscIdAsc(student.getId(), classroom.getId());
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }

        BigDecimal discountAmount = ZERO;
        BigDecimal adjustmentAmount = ZERO;
        BigDecimal finalAmount = tuitionPackage.getPrice();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setClassroom(classroom);
        enrollment.setStartDate(learningStartDate);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setCreationSource(EnrollmentCreationSource.LEGACY_IMPORT);
        enrollment.setSelectedPackage(tuitionPackage);
        enrollment.setPackageNameSnapshot(tuitionPackage.getName());
        enrollment.setTotalSessions(tuitionPackage.getTotalSessions());
        enrollment.setUsedSessions(0);
        enrollment.setTotalSessionsSnapshot(tuitionPackage.getTotalSessions());
        enrollment.setPackagePriceSnapshot(tuitionPackage.getPrice());
        enrollment.setDiscountAmount(discountAmount);
        enrollment.setFinalAmount(finalAmount);
        enrollment.setNote("Nhập liệu legacy Excel");
        enrollment = enrollmentRepository.save(enrollment);

        createHistory(enrollment, EnrollmentStatus.ACTIVE, learningStartDate, null, "Nhập liệu legacy");

        StudentPackage studentPackage = createStudentPackage(
                enrollment,
                student,
                classroom,
                tuitionPackage,
                discountAmount,
                adjustmentAmount,
                finalAmount,
                StudentPackageSourceType.LEGACY_IMPORT
        );
        studentPackage = studentPackageRepository.save(studentPackage);

        Invoice invoice = createInvoice(
                enrollment,
                studentPackage,
                student,
                classroom,
                tuitionPackage,
                discountAmount,
                adjustmentAmount,
                finalAmount,
                learningStartDate,
                learningStartDate
        );
        invoice.setNote("Nhập liệu legacy Excel");
        invoiceRepository.save(invoice);

        return enrollment;
    }

    /**
     * @deprecated use {@link #enrollFromLegacyImport}
     */
    @Transactional
    public Enrollment createFromLegacyImport(
            Student student,
            Classroom classroom,
            TuitionPackage tuitionPackage,
            LocalDate learningStartDate
    ) {
        return enrollFromLegacyImport(student, classroom, tuitionPackage, learningStartDate);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getEligibleStudents(Long classroomId) {
        findClassroom(classroomId);

        return studentRepository.findEligibleForEnrollment(classroomId)
                .stream()
                .map(studentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getEnrollments(int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return enrollmentRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(enrollmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse getById(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Enrollment not found"));
        StudentPackage studentPackage = findLatestStudentPackage(id);
        Invoice invoice = findLatestInvoice(id);

        return enrollmentMapper.toResponse(enrollment, studentPackage, invoice);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentStatusHistoryResponse> getStatusHistory(Long id) {
        if (!enrollmentRepository.existsById(id)) {
            throw new NotFoundException("Enrollment not found");
        }
        return statusHistoryRepository.findByEnrollmentIdOrderByEffectiveFromAscIdAsc(id)
                .stream()
                .map(history -> new EnrollmentStatusHistoryResponse(
                        history.getId(),
                        history.getEnrollment().getId(),
                        history.getStatus(),
                        history.getEffectiveFrom(),
                        history.getEffectiveTo(),
                        history.getReason(),
                        history.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DuplicateEnrollmentGroupResponse> getDuplicates() {
        Map<String, List<Enrollment>> groups = new LinkedHashMap<>();
        for (Enrollment enrollment : enrollmentRepository.findDuplicateStudentClassroomEnrollments()) {
            String key = enrollment.getStudent().getId() + ":" + enrollment.getClassroom().getId();
            groups.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(enrollment);
        }

        return groups.values().stream().map(group -> {
            Enrollment first = group.getFirst();
            List<Long> ids = group.stream().map(Enrollment::getId).toList();
            LocalDate from = group.stream().map(Enrollment::getStartDate).min(LocalDate::compareTo).orElseThrow();
            LocalDate to = group.stream().anyMatch(enrollment -> enrollment.getEndDate() == null)
                    ? null
                    : group.stream().map(Enrollment::getEndDate).max(LocalDate::compareTo).orElse(null);
            // Attendance has no enrollment_id. Count once over the group's combined date window;
            // overlapping enrollment ranges therefore remain an explicit cleanup approximation.
            long attendanceCount = attendanceRepository.countForEnrollmentPeriod(
                    first.getStudent().getId(),
                    first.getClassroom().getId(),
                    from,
                    to
            );
            long invoiceCount = invoiceRepository.countByEnrollmentIdIn(ids);
            long validPaymentCount = paymentRepository.countValidByEnrollmentIds(ids);
            return new DuplicateEnrollmentGroupResponse(
                    first.getStudent().getId(),
                    first.getClassroom().getId(),
                    ids,
                    group.stream().map(Enrollment::getStatus).toList(),
                    attendanceCount,
                    invoiceCount,
                    validPaymentCount,
                    attendanceCount > 0 || invoiceCount > 0 || validPaymentCount > 0
            );
        }).toList();
    }

    @Transactional
    public EnrollmentResponse stop(Long id, StopEnrollmentRequest request) {
        Enrollment enrollment = findEnrollmentForLifecycle(
                id,
                List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.ON_HOLD)
        );
        LocalDate effectiveDate = request.effectiveDate() == null ? LocalDate.now() : request.effectiveDate();

        transitionHistory(enrollment, effectiveDate, EnrollmentStatus.STOPPED, request.reason(), null);
        enrollment.setStatus(EnrollmentStatus.STOPPED);
        enrollment.setEndDate(effectiveDate);
        enrollment.setNote(appendLifecycleReason(enrollment.getNote(), "Ngừng học", request.reason()));
        enrollmentRepository.save(enrollment);
        return enrollmentMapper.toResponse(
                enrollment,
                findLatestStudentPackage(id),
                findLatestInvoice(id)
        );
    }

    @Transactional
    public EnrollmentResponse hold(Long id, HoldEnrollmentRequest request) {
        Enrollment enrollment = findEnrollmentForLifecycle(id, List.of(EnrollmentStatus.ACTIVE));
        LocalDate effectiveDate = request.effectiveDate() == null ? LocalDate.now() : request.effectiveDate();
        if (request.expectedReturnDate() != null
                && !request.expectedReturnDate().isAfter(effectiveDate)) {
            throw new BusinessException("Expected return date must be after effective date");
        }

        transitionHistory(
                enrollment,
                effectiveDate,
                EnrollmentStatus.ON_HOLD,
                request.reason(),
                request.expectedReturnDate()
        );
        enrollment.setStatus(EnrollmentStatus.ON_HOLD);
        enrollment.setEndDate(null);
        enrollment.setNote(appendLifecycleReason(enrollment.getNote(), "Bảo lưu", request.reason()));
        enrollmentRepository.save(enrollment);
        return enrollmentMapper.toResponse(
                enrollment,
                findLatestStudentPackage(id),
                findLatestInvoice(id)
        );
    }

    @Transactional
    public EnrollmentResponse reactivate(Long id, ReactivateEnrollmentRequest request) {
        Enrollment enrollment = findEnrollmentForLifecycle(
                id,
                List.of(EnrollmentStatus.ON_HOLD, EnrollmentStatus.STOPPED)
        );
        LocalDate requestedDate = request.effectiveDate() == null ? LocalDate.now() : request.effectiveDate();
        validateClassroomOpenForEnrollment(enrollment.getClassroom());
        if (enrollmentRepository.existsByStudentIdAndClassroomIdAndStatusAndIdNot(
                enrollment.getStudent().getId(),
                enrollment.getClassroom().getId(),
                EnrollmentStatus.ACTIVE,
                enrollment.getId()
        )) {
            throw new BusinessException("Học viên đang học trong lớp này.");
        }

        // Store the first attendance-eligible session date (half-open ACTIVE from this day).
        // Office-visit dates that are not study days snap forward to the next non-canceled session.
        LocalDate effectiveDate = resolveReactivationEffectiveDate(enrollment.getClassroom(), requestedDate);

        transitionHistory(enrollment, effectiveDate, EnrollmentStatus.ACTIVE, request.reason(), null);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setEndDate(null);
        enrollment.setNote(appendLifecycleReason(enrollment.getNote(), "Học lại", request.reason()));
        enrollmentRepository.save(enrollment);
        return enrollmentMapper.toResponse(
                enrollment,
                findLatestStudentPackage(id),
                findLatestInvoice(id)
        );
    }

    @Transactional
    public TransferEnrollmentResponse transfer(Long id, TransferEnrollmentRequest request) {
        Enrollment source = findEnrollmentForLifecycle(id, List.of(EnrollmentStatus.ACTIVE));
        Classroom targetClassroom = findClassroom(request.targetClassroomId());
        validateClassroomOpenForEnrollment(targetClassroom);
        if (source.getClassroom().getId().equals(targetClassroom.getId())) {
            throw new BusinessException("Target classroom must be different from source classroom");
        }
        validateNoExistingEnrollment(source.getStudent().getId(), targetClassroom.getId());

        EnrollmentLearningDateHelper.validateLearningStartDate(
                targetClassroom,
                request.targetLearningStartDate()
        );
        validateLearningStartDateAgainstExistingSessions(
                targetClassroom.getId(),
                request.targetLearningStartDate()
        );

        int transferredSessions = Math.max(source.getTotalSessions() - source.getUsedSessions(), 0);
        transitionHistory(
                source,
                request.targetLearningStartDate(),
                EnrollmentStatus.TRANSFERRED,
                request.reason(),
                null
        );
        source.setStatus(EnrollmentStatus.TRANSFERRED);
        source.setEndDate(request.targetLearningStartDate().minusDays(1));
        source.setNote(appendLifecycleReason(source.getNote(), "Chuyển lớp", request.reason()));
        source = enrollmentRepository.save(source);

        Enrollment target = new Enrollment();
        target.setStudent(source.getStudent());
        target.setClassroom(targetClassroom);
        target.setStartDate(request.targetLearningStartDate());
        target.setStatus(EnrollmentStatus.ACTIVE);
        target.setCreationSource(
                source.getCreationSource() != null
                        ? source.getCreationSource()
                        : EnrollmentCreationSource.ENROLLMENT
        );
        target.setSelectedPackage(source.getSelectedPackage());
        target.setPackageNameSnapshot(source.getPackageNameSnapshot());
        target.setTotalSessions(transferredSessions);
        target.setUsedSessions(0);
        target.setTotalSessionsSnapshot(source.getTotalSessionsSnapshot());
        target.setPackagePriceSnapshot(source.getPackagePriceSnapshot());
        target.setDiscountAmount(source.getDiscountAmount());
        target.setFinalAmount(source.getFinalAmount());
        target.setNote(appendLifecycleReason(null, "Chuyển từ ghi danh #" + source.getId(), request.reason()));
        target = enrollmentRepository.save(target);
        createHistory(
                target,
                EnrollmentStatus.ACTIVE,
                request.targetLearningStartDate(),
                null,
                "Chuyển từ ghi danh #" + source.getId() + ": " + request.reason().trim()
        );

        String warning = transferredSessions == 0
                ? "Hết buổi - cần gia hạn"
                : null;
        return new TransferEnrollmentResponse(
                enrollmentMapper.toResponse(
                        source,
                        findLatestStudentPackage(source.getId()),
                        findLatestInvoice(source.getId())
                ),
                enrollmentMapper.toResponse(target),
                transferredSessions,
                warning
        );
    }

    @Transactional
    public EnrollmentResponse cancel(Long id, CancelEnrollmentRequest request) {
        Enrollment enrollment = findEnrollmentForLifecycle(id, List.of(EnrollmentStatus.ACTIVE));
        if (attendanceRepository.countForEnrollmentPeriod(
                enrollment.getStudent().getId(),
                enrollment.getClassroom().getId(),
                enrollment.getStartDate(),
                enrollment.getEndDate()
        ) > 0) {
            throw new BusinessException(
                    "Ghi danh đã có dữ liệu điểm danh. Vui lòng dùng Ngừng học."
            );
        }

        List<Invoice> invoices = invoiceRepository.findAllByEnrollmentIdOrderByCreatedAtDesc(id);
        if (invoices.stream().anyMatch(invoice ->
                paymentRepository.existsByInvoiceIdAndStatus(invoice.getId(), PaymentStatus.VALID))) {
            throw new BusinessException(
                    "Ghi danh đã có thanh toán hợp lệ. Không thể hủy ghi danh."
            );
        }

        transitionHistory(
                enrollment,
                LocalDate.now(),
                EnrollmentStatus.CANCELED,
                request.reason(),
                null
        );
        enrollment.setStatus(EnrollmentStatus.CANCELED);
        enrollment.setNote(appendLifecycleReason(enrollment.getNote(), "Hủy ghi danh", request.reason()));
        enrollmentRepository.save(enrollment);

        List<StudentPackage> studentPackages = studentPackageRepository.findAllByEnrollmentId(id);
        studentPackages.forEach(studentPackage -> studentPackage.setStatus(StudentPackageStatus.CANCELED));
        studentPackageRepository.saveAll(studentPackages);

        LocalDateTime canceledAt = LocalDateTime.now();
        invoices.stream()
                .filter(invoice -> invoice.getStudentPackage() != null
                        && invoice.getStudentPackage().getSourceType() == StudentPackageSourceType.ENROLLMENT)
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.UNPAID
                        || invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID)
                .forEach(invoice -> {
                    invoice.setStatus(InvoiceStatus.CANCELED);
                    invoice.setCancelReason(request.reason().trim());
                    invoice.setCanceledAt(canceledAt);
                });
        invoiceRepository.saveAll(invoices);

        StudentPackage responsePackage = studentPackages.isEmpty() ? null : studentPackages.getFirst();
        Invoice responseInvoice = invoices.isEmpty() ? null : invoices.getFirst();
        return enrollmentMapper.toResponse(enrollment, responsePackage, responseInvoice);
    }

    private Enrollment createEnrollment(
            EnrollStudentRequest request,
            Student student,
            Classroom classroom,
            TuitionPackage tuitionPackage,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            LocalDate learningStartDate,
            EnrollmentCreationSource creationSource
    ) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setClassroom(classroom);
        enrollment.setStartDate(learningStartDate);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setCreationSource(creationSource);
        enrollment.setSelectedPackage(tuitionPackage);
        enrollment.setPackageNameSnapshot(tuitionPackage.getName());
        enrollment.setTotalSessions(tuitionPackage.getTotalSessions());
        enrollment.setUsedSessions(0);
        enrollment.setTotalSessionsSnapshot(tuitionPackage.getTotalSessions());
        enrollment.setPackagePriceSnapshot(tuitionPackage.getPrice());
        enrollment.setDiscountAmount(discountAmount);
        enrollment.setFinalAmount(finalAmount);
        enrollment.setNote(trimToNull(request.note()));
        return enrollment;
    }

    private StudentPackage createStudentPackage(
            Enrollment enrollment,
            Student student,
            Classroom classroom,
            TuitionPackage tuitionPackage,
            BigDecimal discountAmount,
            BigDecimal adjustmentAmount,
            BigDecimal finalAmount,
            StudentPackageSourceType sourceType
    ) {
        StudentPackage studentPackage = new StudentPackage();
        studentPackage.setStudent(student);
        studentPackage.setClassroom(classroom);
        studentPackage.setEnrollment(enrollment);
        studentPackage.setTuitionPackage(tuitionPackage);
        studentPackage.setPackageName(tuitionPackage.getName());
        studentPackage.setTotalSessions(tuitionPackage.getTotalSessions());
        studentPackage.setPrice(tuitionPackage.getPrice());
        studentPackage.setDiscountAmount(discountAmount);
        studentPackage.setAdjustmentAmount(adjustmentAmount);
        studentPackage.setFinalAmount(finalAmount);
        studentPackage.setStartDate(enrollment.getStartDate());
        studentPackage.setStatus(StudentPackageStatus.CONFIRMED);
        studentPackage.setSourceType(sourceType);
        studentPackage.setCycleNo(1);
        return studentPackage;
    }

    private Invoice createInvoice(
            Enrollment enrollment,
            StudentPackage studentPackage,
            Student student,
            Classroom classroom,
            TuitionPackage tuitionPackage,
            BigDecimal discountAmount,
            BigDecimal adjustmentAmount,
            BigDecimal finalAmount,
            LocalDate issueDate
    ) {
        return createInvoice(
                enrollment,
                studentPackage,
                student,
                classroom,
                tuitionPackage,
                discountAmount,
                adjustmentAmount,
                finalAmount,
                issueDate,
                null
        );
    }

    private Invoice createInvoice(
            Enrollment enrollment,
            StudentPackage studentPackage,
            Student student,
            Classroom classroom,
            TuitionPackage tuitionPackage,
            BigDecimal discountAmount,
            BigDecimal adjustmentAmount,
            BigDecimal finalAmount,
            LocalDate issueDate,
            LocalDate overrideDueDate
    ) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceCode(generateInvoiceCode());
        invoice.setStudent(student);
        invoice.setClassroom(classroom);
        invoice.setEnrollment(enrollment);
        invoice.setStudentPackage(studentPackage);
        invoice.setAmount(tuitionPackage.getPrice());
        invoice.setDiscountAmount(discountAmount);
        invoice.setAdjustmentAmount(adjustmentAmount);
        invoice.setFinalAmount(finalAmount);
        invoice.setPaidAmount(ZERO);
        invoice.setRemainingAmount(finalAmount);
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoiceBillingSnapshotService.applyBillingSnapshot(
                invoice,
                studentPackage,
                tuitionPackage,
                issueDate,
                overrideDueDate
        );
        return invoice;
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found"));
    }

    private Enrollment findEnrollmentForLifecycle(Long id, List<EnrollmentStatus> allowedStatuses) {
        Enrollment enrollment = enrollmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Enrollment not found"));
        if (!allowedStatuses.contains(enrollment.getStatus())) {
            throw new BusinessException("Enrollment status does not allow this action");
        }
        return enrollment;
    }

    private void transitionHistory(
            Enrollment enrollment,
            LocalDate effectiveDate,
            EnrollmentStatus newStatus,
            String reason,
            LocalDate newEffectiveTo
    ) {
        EnrollmentStatusHistory latest = statusHistoryRepository.latestForUpdate(enrollment.getId())
                .orElseThrow(() -> new BusinessException("Enrollment status history is missing"));
        if (latest.getStatus() != enrollment.getStatus()) {
            throw new BusinessException("Enrollment current status does not match its latest history");
        }
        if (enrollment.getStatus() == EnrollmentStatus.ACTIVE && latest.getEffectiveTo() != null) {
            throw new BusinessException("Active enrollment must have exactly one open history period");
        }
        // Half-open [from, to): if the action happens before the period starts
        // (e.g. cancel/stop in July while learning starts in August), collapse the
        // not-yet-started period to an empty window so it never applies.
        if (effectiveDate.isBefore(latest.getEffectiveFrom())) {
            if (newStatus == EnrollmentStatus.ACTIVE) {
                throw new BusinessException("Effective date must not be before the current period start date");
            }
            latest.setEffectiveTo(latest.getEffectiveFrom());
        } else {
            latest.setEffectiveTo(effectiveDate);
        }
        statusHistoryRepository.saveAndFlush(latest);
        createHistory(enrollment, newStatus, effectiveDate, newEffectiveTo, reason);
    }

    private EnrollmentStatusHistory createHistory(
            Enrollment enrollment,
            EnrollmentStatus status,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String reason
    ) {
        EnrollmentStatusHistory history = new EnrollmentStatusHistory();
        history.setEnrollment(enrollment);
        history.setStatus(status);
        history.setEffectiveFrom(effectiveFrom);
        history.setEffectiveTo(effectiveTo);
        history.setReason(trimToNull(reason));
        return statusHistoryRepository.save(history);
    }

    private StudentPackage findLatestStudentPackage(Long enrollmentId) {
        return studentPackageRepository
                .findTopByEnrollmentIdOrderByCycleNoDescIdDesc(enrollmentId)
                .orElse(null);
    }

    private Invoice findLatestInvoice(Long enrollmentId) {
        return invoiceRepository
                .findTopByEnrollmentIdOrderByCreatedAtDesc(enrollmentId)
                .orElse(null);
    }

    private Classroom findClassroom(Long classroomId) {
        return classroomRepository.findById(classroomId)
                .orElseThrow(() -> new NotFoundException("Classroom not found"));
    }

    private TuitionPackage findTuitionPackage(Long tuitionPackageId) {
        return tuitionPackageRepository.findById(tuitionPackageId)
                .orElseThrow(() -> new NotFoundException("Tuition package not found"));
    }

    private void validateTuitionPackageBelongsToClassroom(Long classroomId, Long tuitionPackageId) {
        if (!classPackageRepository.existsByClassroomIdAndTuitionPackageIdAndActiveTrue(
                classroomId,
                tuitionPackageId
        )) {
            throw new BusinessException("Tuition package is not linked to this classroom");
        }
    }

    private void validateTuitionPackageActive(TuitionPackage tuitionPackage) {
        if (tuitionPackage.getStatus() != TuitionPackageStatus.ACTIVE) {
            throw new BusinessException("Tuition package must be active");
        }
    }

    private void validateStudentEligible(Student student) {
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new BusinessException("Only active students can be enrolled");
        }
    }

    private void validateClassroomOpenForEnrollment(Classroom classroom) {
        if (classroom.getStatus() != ClassroomStatus.PLANNED
                && classroom.getStatus() != ClassroomStatus.ONGOING) {
            throw new BusinessException("Cannot enroll students into completed or canceled classroom");
        }
    }

    private void validateNoExistingEnrollment(Long studentId, Long classroomId) {
        Enrollment duplicate = enrollmentRepository
                .findFirstByStudentIdAndClassroomIdAndStatusInOrderByIdDesc(
                        studentId,
                        classroomId,
                        List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.ON_HOLD, EnrollmentStatus.STOPPED)
                ).orElse(null);
        if (duplicate == null) {
            validateCanceledEnrollmentsCanBeReplaced(
                    enrollmentRepository.findByStudentIdAndClassroomIdOrderByStartDateAscIdAsc(
                            studentId,
                            classroomId
                    )
            );
            return;
        }
        if (duplicate.getStatus() == EnrollmentStatus.ACTIVE) {
            throw new BusinessException("Học viên đang học trong lớp này.");
        }
        if (duplicate.getStatus() == EnrollmentStatus.ON_HOLD) {
            throw new BusinessException(
                    "Học viên đang bảo lưu trong lớp này. Vui lòng sử dụng chức năng Học lại."
            );
        }
        throw new BusinessException(
                "Học viên đã từng học lớp này. Vui lòng sử dụng chức năng Học lại."
        );
    }

    private void validateCanceledEnrollmentsCanBeReplaced(List<Enrollment> existingEnrollments) {
        for (Enrollment enrollment : existingEnrollments) {
            if (enrollment.getStatus() != EnrollmentStatus.CANCELED) {
                continue;
            }
            long attendanceCount = attendanceRepository.countForEnrollmentPeriod(
                    enrollment.getStudent().getId(),
                    enrollment.getClassroom().getId(),
                    enrollment.getStartDate(),
                    enrollment.getEndDate()
            );
            boolean hasValidPayment = invoiceRepository
                    .findAllByEnrollmentIdOrderByCreatedAtDesc(enrollment.getId())
                    .stream()
                    .anyMatch(invoice -> paymentRepository.existsByInvoiceIdAndStatus(
                            invoice.getId(),
                            PaymentStatus.VALID
                    ));
            if (attendanceCount > 0 || hasValidPayment) {
                throw new BusinessException(
                        "Ghi danh đã hủy có dữ liệu lịch sử. Vui lòng kiểm tra trước khi ghi danh lại."
                );
            }
        }
    }

    private void validateFinalAmount(BigDecimal price, BigDecimal discountAmount) {
        if (discountAmount.compareTo(ZERO) < 0) {
            throw new BusinessException("Discount amount must be greater than or equal to 0");
        }

        if (discountAmount.compareTo(price) > 0) {
            throw new BusinessException("Discount amount must not exceed package price");
        }
    }

    private LocalDate resolveLearningStartDate(Classroom classroom, LocalDate requestedLearningStartDate) {
        if (requestedLearningStartDate == null) {
            return EnrollmentLearningDateHelper.findFirstValidLearningDate(
                    classroom,
                    classroom.getStartDate()
            );
        }

        EnrollmentLearningDateHelper.validateLearningStartDate(classroom, requestedLearningStartDate);
        validateLearningStartDateAgainstExistingSessions(classroom.getId(), requestedLearningStartDate);
        return requestedLearningStartDate;
    }

    /**
     * Resolves reactivation to the first attendance-eligible date on or after the requested date.
     * Prefer an existing non-canceled class session; otherwise fall back to classroom study days.
     */
    private LocalDate resolveReactivationEffectiveDate(Classroom classroom, LocalDate requestedDate) {
        List<ClassSession> sessions = classSessionRepository
                .findByClassroomIdOrderBySessionDateAscStartTimeAsc(classroom.getId());
        if (sessions != null && !sessions.isEmpty()) {
            return sessions.stream()
                    .filter(session -> session.getStatus() != ClassSessionStatus.CANCELED)
                    .map(ClassSession::getSessionDate)
                    .filter(sessionDate -> !sessionDate.isBefore(requestedDate))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            "Không còn buổi học hợp lệ để học lại từ ngày đã chọn."
                    ));
        }

        return EnrollmentLearningDateHelper.findFirstValidLearningDate(classroom, requestedDate);
    }

    private void validateLearningStartDateAgainstExistingSessions(Long classroomId, LocalDate learningStartDate) {
        if (classSessionRepository.countByClassroomId(classroomId) == 0) {
            return;
        }

        if (!classSessionRepository.existsByClassroomIdAndSessionDateAndStatusNot(
                classroomId,
                learningStartDate,
                ClassSessionStatus.CANCELED
        )) {
            throw new BusinessException(
                    EnrollmentLearningDateHelper.LEARNING_START_MUST_MATCH_SESSION_MESSAGE
            );
        }
    }

    private BigDecimal normalizeDiscountAmount(BigDecimal discountAmount) {
        return discountAmount == null ? ZERO : discountAmount;
    }

    private String generateInvoiceCode() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String appendLifecycleReason(String currentNote, String action, String reason) {
        if (trimToNull(reason) == null) {
            return currentNote;
        }
        String entry = action + ": " + reason.trim();
        String combined = trimToNull(currentNote) == null ? entry : currentNote.trim() + "\n" + entry;
        return combined.length() <= 1000 ? combined : combined.substring(combined.length() - 1000);
    }
}
