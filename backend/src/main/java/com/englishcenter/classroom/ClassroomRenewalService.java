package com.englishcenter.classroom;

import com.englishcenter.classpackage.ClassPackage;
import com.englishcenter.classpackage.ClassPackageRepository;
import com.englishcenter.classroom.dto.ClassroomRenewalCandidateResponse;
import com.englishcenter.classroom.dto.ClassroomRenewalConfirmItemResponse;
import com.englishcenter.classroom.dto.ClassroomRenewalConfirmResponse;
import com.englishcenter.classroom.dto.ClassroomRenewalItemRequest;
import com.englishcenter.classroom.dto.ClassroomRenewalPreviewItemResponse;
import com.englishcenter.classroom.dto.ClassroomRenewalPreviewResponse;
import com.englishcenter.classroom.dto.ClassroomRenewalRequest;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentSessionService;
import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.studentpackage.StudentPackage;
import com.englishcenter.studentpackage.StudentPackageRepository;
import com.englishcenter.studentpackage.StudentPackageSourceType;
import com.englishcenter.studentpackage.StudentPackageStatus;
import com.englishcenter.tuitionpackage.TuitionPackage;
import com.englishcenter.tuitionpackage.TuitionPackageRepository;
import com.englishcenter.tuitionpackage.TuitionPackageStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassroomRenewalService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int ALL_THRESHOLD = 999_999;

    private final ClassroomRepository classroomRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentPackageRepository studentPackageRepository;
    private final TuitionPackageRepository tuitionPackageRepository;
    private final ClassPackageRepository classPackageRepository;
    private final InvoiceRepository invoiceRepository;
    private final EnrollmentSessionService enrollmentSessionService;

    public ClassroomRenewalService(
            ClassroomRepository classroomRepository,
            EnrollmentRepository enrollmentRepository,
            StudentPackageRepository studentPackageRepository,
            TuitionPackageRepository tuitionPackageRepository,
            ClassPackageRepository classPackageRepository,
            InvoiceRepository invoiceRepository,
            EnrollmentSessionService enrollmentSessionService
    ) {
        this.classroomRepository = classroomRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentPackageRepository = studentPackageRepository;
        this.tuitionPackageRepository = tuitionPackageRepository;
        this.classPackageRepository = classPackageRepository;
        this.invoiceRepository = invoiceRepository;
        this.enrollmentSessionService = enrollmentSessionService;
    }

    @Transactional(readOnly = true)
    public List<ClassroomRenewalCandidateResponse> getCandidates(Long classroomId, int remainingThreshold) {
        findClassroom(classroomId);
        int normalizedThreshold = remainingThreshold < 0 ? ALL_THRESHOLD : remainingThreshold;
        List<Enrollment> enrollments = enrollmentRepository.findByClassroomIdAndStatus(
                classroomId,
                EnrollmentStatus.ACTIVE
        );
        Map<Long, StudentPackage> latestPackagesByEnrollmentId = latestPackagesByEnrollment(classroomId);
        Long fallbackPackageId = firstActiveClassPackageId(classroomId);

        return enrollments.stream()
                .map(enrollment -> toCandidate(
                        enrollment,
                        latestPackagesByEnrollmentId.get(enrollment.getId()),
                        fallbackPackageId
                ))
                .filter(candidate -> normalizedThreshold >= ALL_THRESHOLD
                        || candidate.remainingSessions() <= normalizedThreshold)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClassroomRenewalPreviewResponse preview(Long classroomId, ClassroomRenewalRequest request) {
        findClassroom(classroomId);
        validateNoDuplicateEnrollments(request.items());

        List<RenewalContext> contexts = request.items().stream()
                .map(item -> validateItem(classroomId, item))
                .toList();

        List<ClassroomRenewalPreviewItemResponse> items = contexts.stream()
                .map(this::toPreviewItem)
                .toList();

        return new ClassroomRenewalPreviewResponse(
                items.size(),
                sumInvoiceAmount(items),
                items
        );
    }

    @Transactional
    public ClassroomRenewalConfirmResponse confirm(Long classroomId, ClassroomRenewalRequest request) {
        findClassroom(classroomId);
        validateNoDuplicateEnrollments(request.items());

        List<RenewalContext> contexts = request.items().stream()
                .map(item -> validateItem(classroomId, item))
                .toList();

        List<ClassroomRenewalConfirmItemResponse> items = contexts.stream()
                .map(this::confirmOne)
                .toList();

        return new ClassroomRenewalConfirmResponse(
                items.size(),
                items.stream()
                        .map(ClassroomRenewalConfirmItemResponse::newInvoiceAmount)
                        .reduce(ZERO, BigDecimal::add),
                items
        );
    }

    private ClassroomRenewalCandidateResponse toCandidate(
            Enrollment enrollment,
            StudentPackage latestPackage,
            Long fallbackPackageId
    ) {
        int usedSessions = enrollment.getUsedSessions();
        int remainingSessions = enrollmentSessionService.remainingSessions(enrollment);
        String packageName = latestPackage == null
                ? enrollment.getPackageNameSnapshot()
                : latestPackage.getPackageName();
        Integer packageTotalSessions = latestPackage == null
                ? enrollment.getTotalSessionsSnapshot()
                : latestPackage.getTotalSessions();
        Long suggestedPackageId = latestPackage != null
                && latestPackage.getTuitionPackage().getStatus() == TuitionPackageStatus.ACTIVE
                ? latestPackage.getTuitionPackage().getId()
                : fallbackPackageId;

        return new ClassroomRenewalCandidateResponse(
                enrollment.getStudent().getId(),
                enrollment.getId(),
                latestPackage == null ? null : latestPackage.getId(),
                enrollment.getStudent().getFullName(),
                packageName,
                packageTotalSessions,
                usedSessions,
                remainingSessions,
                false,
                null,
                suggestedPackageId,
                suggestedPackageId != null,
                null
        );
    }

    private ClassroomRenewalPreviewItemResponse toPreviewItem(RenewalContext context) {
        StudentPackage latestPackage = context.latestPackage();
        return new ClassroomRenewalPreviewItemResponse(
                context.enrollment().getStudent().getId(),
                context.enrollment().getId(),
                latestPackage == null ? null : latestPackage.getId(),
                context.enrollment().getStudent().getFullName(),
                latestPackage == null
                        ? context.enrollment().getPackageNameSnapshot()
                        : latestPackage.getPackageName(),
                context.enrollment().getUsedSessions(),
                enrollmentSessionService.remainingSessions(context.enrollment()),
                context.tuitionPackage().getId(),
                context.tuitionPackage().getName(),
                context.tuitionPackage().getTotalSessions(),
                context.tuitionPackage().getPrice(),
                StudentPackageStatus.CONFIRMED,
                true,
                null
        );
    }

    private ClassroomRenewalConfirmItemResponse confirmOne(RenewalContext context) {
        Enrollment enrollment = context.enrollment();
        TuitionPackage tuitionPackage = context.tuitionPackage();
        LocalDate today = LocalDate.now();

        enrollment.setTotalSessions(enrollment.getTotalSessions() + tuitionPackage.getTotalSessions());
        enrollmentRepository.save(enrollment);

        StudentPackage newStudentPackage = createStudentPackage(context, today);
        newStudentPackage = studentPackageRepository.save(newStudentPackage);

        Invoice invoice = createInvoice(context, newStudentPackage, today);
        invoice = invoiceRepository.save(invoice);

        return new ClassroomRenewalConfirmItemResponse(
                enrollment.getStudent().getId(),
                enrollment.getId(),
                enrollment.getStudent().getFullName(),
                newStudentPackage.getId(),
                invoice.getId(),
                newStudentPackage.getPackageName(),
                invoice.getFinalAmount(),
                newStudentPackage.getStatus()
        );
    }

    private RenewalContext validateItem(Long classroomId, ClassroomRenewalItemRequest item) {
        Enrollment enrollment = enrollmentRepository.findById(item.enrollmentId())
                .orElseThrow(() -> new NotFoundException("Enrollment not found"));
        if (!enrollment.getClassroom().getId().equals(classroomId) || enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BusinessException("Học viên không có ghi danh đang hoạt động trong lớp này");
        }

        TuitionPackage tuitionPackage = tuitionPackageRepository.findById(item.tuitionPackageId())
                .orElseThrow(() -> new NotFoundException("Tuition package not found"));
        if (tuitionPackage.getStatus() != TuitionPackageStatus.ACTIVE) {
            throw new BusinessException("Gói học phí phải đang hoạt động");
        }
        if (!classPackageRepository.existsByClassroomIdAndTuitionPackageIdAndActiveTrue(
                classroomId,
                tuitionPackage.getId()
        )) {
            throw new BusinessException("Gói học phí không được áp dụng cho lớp này");
        }

        StudentPackage latestPackage = studentPackageRepository
                .findTopByEnrollmentIdOrderByCycleNoDescIdDesc(enrollment.getId())
                .orElse(null);

        return new RenewalContext(enrollment, latestPackage, tuitionPackage);
    }

    private StudentPackage createStudentPackage(RenewalContext context, LocalDate startDate) {
        TuitionPackage tuitionPackage = context.tuitionPackage();
        StudentPackage studentPackage = new StudentPackage();
        studentPackage.setStudent(context.enrollment().getStudent());
        studentPackage.setClassroom(context.enrollment().getClassroom());
        studentPackage.setEnrollment(context.enrollment());
        studentPackage.setTuitionPackage(tuitionPackage);
        studentPackage.setPackageName(tuitionPackage.getName());
        studentPackage.setTotalSessions(tuitionPackage.getTotalSessions());
        studentPackage.setPrice(tuitionPackage.getPrice());
        studentPackage.setDiscountAmount(ZERO);
        studentPackage.setAdjustmentAmount(ZERO);
        studentPackage.setFinalAmount(tuitionPackage.getPrice());
        studentPackage.setStartDate(startDate);
        studentPackage.setStatus(StudentPackageStatus.CONFIRMED);
        studentPackage.setSourceType(StudentPackageSourceType.RENEWAL);
        studentPackage.setCycleNo(
                studentPackageRepository.findMaxCycleNoByEnrollmentId(context.enrollment().getId()) + 1
        );
        return studentPackage;
    }

    private Invoice createInvoice(RenewalContext context, StudentPackage studentPackage, LocalDate dueDate) {
        TuitionPackage tuitionPackage = context.tuitionPackage();
        Invoice invoice = new Invoice();
        invoice.setInvoiceCode(generateInvoiceCode());
        invoice.setStudent(context.enrollment().getStudent());
        invoice.setClassroom(context.enrollment().getClassroom());
        invoice.setEnrollment(context.enrollment());
        invoice.setStudentPackage(studentPackage);
        invoice.setPackageNameSnapshot(tuitionPackage.getName());
        invoice.setTotalSessionsSnapshot(tuitionPackage.getTotalSessions());
        invoice.setAmount(tuitionPackage.getPrice());
        invoice.setDiscountAmount(ZERO);
        invoice.setAdjustmentAmount(ZERO);
        invoice.setFinalAmount(tuitionPackage.getPrice());
        invoice.setPaidAmount(ZERO);
        invoice.setRemainingAmount(tuitionPackage.getPrice());
        invoice.setDueDate(dueDate);
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setNote("Gia hạn gói học phí");
        return invoice;
    }

    private Map<Long, StudentPackage> latestPackagesByEnrollment(Long classroomId) {
        return studentPackageRepository.findByClassroomIdOrderByStartDateDesc(classroomId)
                .stream()
                .collect(Collectors.toMap(
                        studentPackage -> studentPackage.getEnrollment().getId(),
                        Function.identity(),
                        (left, right) -> left.getCycleNo() > right.getCycleNo()
                                || (left.getCycleNo().equals(right.getCycleNo()) && left.getId() > right.getId())
                                ? left
                                : right
                ));
    }

    private Long firstActiveClassPackageId(Long classroomId) {
        return classPackageRepository.findByClassroomIdAndActiveTrueOrderByCreatedAtDesc(classroomId)
                .stream()
                .map(ClassPackage::getTuitionPackage)
                .filter(tuitionPackage -> tuitionPackage.getStatus() == TuitionPackageStatus.ACTIVE)
                .map(TuitionPackage::getId)
                .findFirst()
                .orElse(null);
    }

    private void validateNoDuplicateEnrollments(List<ClassroomRenewalItemRequest> items) {
        Set<Long> enrollmentIds = new HashSet<>();
        for (ClassroomRenewalItemRequest item : items) {
            if (!enrollmentIds.add(item.enrollmentId())) {
                throw new BusinessException("Danh sách gia hạn có học viên bị trùng");
            }
        }
    }

    private Classroom findClassroom(Long classroomId) {
        return classroomRepository.findById(classroomId)
                .orElseThrow(() -> new NotFoundException("Classroom not found"));
    }

    private BigDecimal sumInvoiceAmount(List<ClassroomRenewalPreviewItemResponse> items) {
        return items.stream()
                .map(ClassroomRenewalPreviewItemResponse::newInvoiceAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    private String generateInvoiceCode() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private record RenewalContext(
            Enrollment enrollment,
            StudentPackage latestPackage,
            TuitionPackage tuitionPackage
    ) {
    }
}
