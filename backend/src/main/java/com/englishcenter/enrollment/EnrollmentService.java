package com.englishcenter.enrollment;

import com.englishcenter.classpackage.ClassPackageRepository;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classroom.ClassroomStatus;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.enrollment.dto.EnrollStudentRequest;
import com.englishcenter.enrollment.dto.EnrollmentResponse;
import com.englishcenter.enrollment.mapper.EnrollmentMapper;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceStatus;
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
import java.util.List;
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
    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final TuitionPackageRepository tuitionPackageRepository;
    private final ClassPackageRepository classPackageRepository;
    private final StudentPackageRepository studentPackageRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClassSessionRepository classSessionRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            ClassroomRepository classroomRepository,
            TuitionPackageRepository tuitionPackageRepository,
            ClassPackageRepository classPackageRepository,
            StudentPackageRepository studentPackageRepository,
            InvoiceRepository invoiceRepository,
            ClassSessionRepository classSessionRepository,
            EnrollmentMapper enrollmentMapper,
            StudentMapper studentMapper
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.classroomRepository = classroomRepository;
        this.tuitionPackageRepository = tuitionPackageRepository;
        this.classPackageRepository = classPackageRepository;
        this.studentPackageRepository = studentPackageRepository;
        this.invoiceRepository = invoiceRepository;
        this.classSessionRepository = classSessionRepository;
        this.enrollmentMapper = enrollmentMapper;
        this.studentMapper = studentMapper;
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
                learningStartDate
        );
        enrollment = enrollmentRepository.save(enrollment);

        StudentPackage studentPackage = createStudentPackage(
                enrollment,
                student,
                classroom,
                tuitionPackage,
                discountAmount,
                adjustmentAmount,
                finalAmount
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
        StudentPackage studentPackage = studentPackageRepository.findByEnrollmentId(id).orElse(null);
        Invoice invoice = invoiceRepository.findByEnrollmentId(id).orElse(null);

        return enrollmentMapper.toResponse(enrollment, studentPackage, invoice);
    }

    private Enrollment createEnrollment(
            EnrollStudentRequest request,
            Student student,
            Classroom classroom,
            TuitionPackage tuitionPackage,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            LocalDate learningStartDate
    ) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setClassroom(classroom);
        enrollment.setStartDate(learningStartDate);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
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
            BigDecimal finalAmount
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
        studentPackage.setSourceType(StudentPackageSourceType.ENROLLMENT);
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
            LocalDate dueDate
    ) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceCode(generateInvoiceCode());
        invoice.setStudent(student);
        invoice.setClassroom(classroom);
        invoice.setEnrollment(enrollment);
        invoice.setStudentPackage(studentPackage);
        invoice.setPackageNameSnapshot(tuitionPackage.getName());
        invoice.setTotalSessionsSnapshot(tuitionPackage.getTotalSessions());
        invoice.setAmount(tuitionPackage.getPrice());
        invoice.setDiscountAmount(discountAmount);
        invoice.setAdjustmentAmount(adjustmentAmount);
        invoice.setFinalAmount(finalAmount);
        invoice.setPaidAmount(ZERO);
        invoice.setRemainingAmount(finalAmount);
        invoice.setDueDate(dueDate);
        invoice.setStatus(InvoiceStatus.UNPAID);
        return invoice;
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found"));
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
        if (enrollmentRepository.existsByStudentIdAndClassroomIdAndStatusIn(
                studentId,
                classroomId,
                List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.ON_HOLD)
        )) {
            throw new BusinessException("Student already has an enrollment in this classroom");
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
}
