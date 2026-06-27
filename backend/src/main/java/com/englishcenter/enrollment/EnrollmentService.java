package com.englishcenter.enrollment;

import com.englishcenter.classpackage.ClassPackageRepository;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
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
import com.englishcenter.studentpackage.StudentPackage;
import com.englishcenter.studentpackage.StudentPackageRepository;
import com.englishcenter.studentpackage.StudentPackageStatus;
import com.englishcenter.tuitionpackage.TuitionPackage;
import com.englishcenter.tuitionpackage.TuitionPackageRepository;
import com.englishcenter.tuitionpackage.TuitionPackageStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final EnrollmentMapper enrollmentMapper;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            ClassroomRepository classroomRepository,
            TuitionPackageRepository tuitionPackageRepository,
            ClassPackageRepository classPackageRepository,
            StudentPackageRepository studentPackageRepository,
            InvoiceRepository invoiceRepository,
            EnrollmentMapper enrollmentMapper
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.classroomRepository = classroomRepository;
        this.tuitionPackageRepository = tuitionPackageRepository;
        this.classPackageRepository = classPackageRepository;
        this.studentPackageRepository = studentPackageRepository;
        this.invoiceRepository = invoiceRepository;
        this.enrollmentMapper = enrollmentMapper;
    }

    @Transactional
    public EnrollmentResponse enrollStudent(EnrollStudentRequest request) {
        Student student = findStudent(request.studentId());
        Classroom classroom = findClassroom(request.classroomId());
        TuitionPackage tuitionPackage = findTuitionPackage(request.tuitionPackageId());
        BigDecimal discountAmount = normalizeDiscountAmount(request.discountAmount());

        validateTuitionPackageActive(tuitionPackage);
        validateTuitionPackageBelongsToClassroom(classroom.getId(), tuitionPackage.getId());
        validateNoDuplicateActiveEnrollment(student.getId(), classroom.getId());
        validateFinalAmount(tuitionPackage.getPrice(), discountAmount);

        BigDecimal adjustmentAmount = ZERO;
        BigDecimal finalAmount = tuitionPackage.getPrice().subtract(discountAmount).add(adjustmentAmount);

        Enrollment enrollment = createEnrollment(
                request,
                student,
                classroom,
                tuitionPackage,
                discountAmount,
                finalAmount
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
                request.startDate()
        );
        invoice = invoiceRepository.save(invoice);

        // TODO: Save ActivityLog for ENROLL_STUDENT when the ActivityLog module exists.
        return enrollmentMapper.toResponse(enrollment, studentPackage, invoice);
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
            BigDecimal finalAmount
    ) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setClassroom(classroom);
        enrollment.setStartDate(request.startDate());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setSelectedPackage(tuitionPackage);
        enrollment.setPackageNameSnapshot(tuitionPackage.getName());
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
        studentPackage.setStatus(StudentPackageStatus.ACTIVE);
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

    private void validateNoDuplicateActiveEnrollment(Long studentId, Long classroomId) {
        if (enrollmentRepository.existsByStudentIdAndClassroomIdAndStatus(
                studentId,
                classroomId,
                EnrollmentStatus.ACTIVE
        )) {
            throw new BusinessException("Student already has an active enrollment in this classroom");
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
