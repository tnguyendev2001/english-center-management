package com.englishcenter.packagechange;

import com.englishcenter.classpackage.ClassPackageRepository;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentProgressService;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.enrollment.dto.EnrollmentLearningProgressResponse;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.invoice.mapper.InvoiceMapper;
import com.englishcenter.makeupcredit.MakeupCreditRepository;
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import com.englishcenter.packagechange.dto.ChangePackagePreviewResponse;
import com.englishcenter.packagechange.dto.ChangePackageRequest;
import com.englishcenter.packagechange.dto.ChangePackageResponse;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.studentpackage.StudentPackage;
import com.englishcenter.studentpackage.StudentPackageRepository;
import com.englishcenter.studentpackage.StudentPackageSourceType;
import com.englishcenter.studentpackage.StudentPackageStatus;
import com.englishcenter.tuitionpackage.TuitionPackage;
import com.englishcenter.tuitionpackage.TuitionPackageRepository;
import com.englishcenter.tuitionpackage.TuitionPackageStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PackageChangeService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final StudentPackageRepository studentPackageRepository;
    private final TuitionPackageRepository tuitionPackageRepository;
    private final ClassPackageRepository classPackageRepository;
    private final MakeupCreditRepository makeupCreditRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PackageChangeLogRepository packageChangeLogRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentProgressService enrollmentProgressService;
    private final InvoiceMapper invoiceMapper;

    public PackageChangeService(
            StudentPackageRepository studentPackageRepository,
            TuitionPackageRepository tuitionPackageRepository,
            ClassPackageRepository classPackageRepository,
            MakeupCreditRepository makeupCreditRepository,
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            PackageChangeLogRepository packageChangeLogRepository,
            EnrollmentRepository enrollmentRepository,
            EnrollmentProgressService enrollmentProgressService,
            InvoiceMapper invoiceMapper
    ) {
        this.studentPackageRepository = studentPackageRepository;
        this.tuitionPackageRepository = tuitionPackageRepository;
        this.classPackageRepository = classPackageRepository;
        this.makeupCreditRepository = makeupCreditRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.packageChangeLogRepository = packageChangeLogRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentProgressService = enrollmentProgressService;
        this.invoiceMapper = invoiceMapper;
    }

    @Transactional(readOnly = true)
    public ChangePackagePreviewResponse preview(
            Long oldStudentPackageId,
            Long newTuitionPackageId,
            PackageChangeMode changeMode
    ) {
        StudentPackage oldStudentPackage = findStudentPackage(oldStudentPackageId);
        TuitionPackage newTuitionPackage = findTuitionPackage(newTuitionPackageId);

        validateChangeAllowed(oldStudentPackage, newTuitionPackage);

        return calculate(oldStudentPackage, newTuitionPackage, changeMode);
    }

    @Transactional
    public ChangePackageResponse changePackage(Long oldStudentPackageId, ChangePackageRequest request) {
        StudentPackage oldStudentPackage = findStudentPackageForUpdate(oldStudentPackageId);
        TuitionPackage newTuitionPackage = findTuitionPackage(request.newTuitionPackageId());
        Enrollment enrollment = oldStudentPackage.getEnrollment();

        validateChangeAllowed(oldStudentPackage, newTuitionPackage);
        validateNoExistingPackageChange(oldStudentPackage.getId());
        String reason = trimToNull(request.reason());
        if (reason == null) {
            throw new BusinessException("Package change reason is required");
        }

        int usedSessions = enrollment.getUsedSessions();
        validateModeAllowed(request.changeMode(), usedSessions, newTuitionPackage);
        ChangePackagePreviewResponse calculation = calculate(oldStudentPackage, newTuitionPackage, request.changeMode());
        LocalDate changeDate = LocalDate.now();

        oldStudentPackage.setStatus(StudentPackageStatus.CLOSED);
        oldStudentPackage.setEndDate(changeDate);
        oldStudentPackage = studentPackageRepository.save(oldStudentPackage);
        invoiceRepository.replaceCollectibleInvoicesByStudentPackageId(
                oldStudentPackage.getId(),
                LocalDateTime.now()
        );

        if (request.changeMode() == PackageChangeMode.REPLACEMENT_CHANGE) {
            enrollment.setTotalSessions(newTuitionPackage.getTotalSessions());
        } else {
            enrollment.setTotalSessions(enrollment.getTotalSessions() + newTuitionPackage.getTotalSessions());
        }
        enrollmentRepository.save(enrollment);

        StudentPackage newStudentPackage = createStudentPackage(
                oldStudentPackage,
                newTuitionPackage,
                calculation.changeMode(),
                changeDate
        );
        newStudentPackage = studentPackageRepository.save(newStudentPackage);

        Invoice newInvoice = null;
        if (calculation.collectibleAmount().compareTo(ZERO) > 0) {
            newInvoice = createInvoice(
                    oldStudentPackage,
                    newStudentPackage,
                    newTuitionPackage,
                    calculation.collectibleAmount(),
                    changeDate,
                    reason
            );
            newInvoice = invoiceRepository.save(newInvoice);
        }

        PackageChangeLog log = createLog(oldStudentPackage, newStudentPackage, calculation, reason);
        log = packageChangeLogRepository.save(log);

        EnrollmentLearningProgressResponse progress = enrollmentProgressService.getByEnrollmentId(enrollment.getId());

        return new ChangePackageResponse(
                log.getId(),
                calculation,
                progress,
                newInvoice == null ? null : invoiceMapper.toResponse(newInvoice)
        );
    }

    private ChangePackagePreviewResponse calculate(
            StudentPackage oldStudentPackage,
            TuitionPackage newTuitionPackage,
            PackageChangeMode changeMode
    ) {
        Enrollment enrollment = oldStudentPackage.getEnrollment();
        int usedSessions = enrollment.getUsedSessions();
        validateModeAllowed(changeMode, usedSessions, newTuitionPackage);
        int remainingSessions = Math.max(enrollment.getTotalSessions() - usedSessions, 0);
        int makeupAvailableSessions = makeupCreditRepository.countAvailableMakeupCredits(
                oldStudentPackage.getStudent().getId(),
                oldStudentPackage.getClassroom().getId(),
                MakeupCreditStatus.AVAILABLE
        );
        BigDecimal oldUnitPrice = divideMoney(
                oldStudentPackage.getFinalAmount(),
                BigDecimal.valueOf(oldStudentPackage.getTotalSessions())
        );
        BigDecimal usedAmount = divideMoney(
                oldStudentPackage.getFinalAmount().multiply(BigDecimal.valueOf(usedSessions)),
                BigDecimal.valueOf(oldStudentPackage.getTotalSessions())
        );
        BigDecimal totalValidPaidAmount = getTotalValidPaidAmount(oldStudentPackage.getId());
        BigDecimal unusedCredit = ZERO;
        BigDecimal oldDebt = ZERO;
        BigDecimal amountToPay;
        int remainingSessionsAfterChange;

        if (changeMode == PackageChangeMode.REPLACEMENT_CHANGE) {
            amountToPay = money(newTuitionPackage.getPrice().subtract(totalValidPaidAmount));
            remainingSessionsAfterChange = Math.max(newTuitionPackage.getTotalSessions() - usedSessions, 0);
        } else {
            BigDecimal balanceFromOldPackage = money(totalValidPaidAmount.subtract(usedAmount));
            if (balanceFromOldPackage.compareTo(ZERO) >= 0) {
                unusedCredit = balanceFromOldPackage;
            } else {
                oldDebt = money(balanceFromOldPackage.abs());
            }
            amountToPay = money(newTuitionPackage.getPrice().subtract(unusedCredit).add(oldDebt));
            remainingSessionsAfterChange = Math.max(
                    enrollment.getTotalSessions() + newTuitionPackage.getTotalSessions() - usedSessions,
                    0
            );
        }

        if (amountToPay.compareTo(ZERO) < 0) {
            amountToPay = ZERO;
        }

        BigDecimal collectibleAmount = amountToPay;
        BigDecimal adjustmentAmount = money(totalValidPaidAmount.subtract(usedAmount));
        PackageChangeAdjustmentType adjustmentType = resolveAdjustmentType(adjustmentAmount);
        BigDecimal newInvoiceAdjustmentAmount = money(collectibleAmount.subtract(newTuitionPackage.getPrice()));
        return new ChangePackagePreviewResponse(
                oldStudentPackage.getId(),
                changeMode,
                allowedModes(),
                oldStudentPackage.getPackageName(),
                enrollment.getTotalSessions(),
                money(oldStudentPackage.getPrice()),
                money(oldStudentPackage.getFinalAmount()),
                usedSessions,
                remainingSessions,
                remainingSessionsAfterChange,
                makeupAvailableSessions,
                oldUnitPrice,
                usedAmount,
                totalValidPaidAmount,
                totalValidPaidAmount,
                adjustmentAmount,
                adjustmentType,
                unusedCredit,
                oldDebt,
                newTuitionPackage.getId(),
                newTuitionPackage.getName(),
                newTuitionPackage.getTotalSessions(),
                money(newTuitionPackage.getPrice()),
                amountToPay,
                collectibleAmount,
                newInvoiceAdjustmentAmount,
                collectibleAmount,
                null
        );
    }

    private BigDecimal getTotalValidPaidAmount(Long studentPackageId) {
        return money(paymentRepository.sumValidAmountByStudentPackageId(studentPackageId));
    }

    private StudentPackage createStudentPackage(
            StudentPackage oldStudentPackage,
            TuitionPackage newTuitionPackage,
            PackageChangeMode changeMode,
            LocalDate changeDate
    ) {
        StudentPackage newStudentPackage = new StudentPackage();
        newStudentPackage.setStudent(oldStudentPackage.getStudent());
        newStudentPackage.setClassroom(oldStudentPackage.getClassroom());
        newStudentPackage.setEnrollment(oldStudentPackage.getEnrollment());
        newStudentPackage.setTuitionPackage(newTuitionPackage);
        newStudentPackage.setPackageName(newTuitionPackage.getName());
        newStudentPackage.setTotalSessions(newTuitionPackage.getTotalSessions());
        newStudentPackage.setPrice(newTuitionPackage.getPrice());
        newStudentPackage.setDiscountAmount(ZERO);
        newStudentPackage.setAdjustmentAmount(ZERO);
        newStudentPackage.setFinalAmount(newTuitionPackage.getPrice());
        newStudentPackage.setStartDate(
                changeMode == PackageChangeMode.REPLACEMENT_CHANGE ? oldStudentPackage.getStartDate() : changeDate
        );
        newStudentPackage.setStatus(StudentPackageStatus.CONFIRMED);
        newStudentPackage.setSourceType(
                changeMode == PackageChangeMode.REPLACEMENT_CHANGE
                        ? StudentPackageSourceType.PACKAGE_CHANGE_REPLACEMENT
                        : StudentPackageSourceType.PACKAGE_CHANGE_NEW_CYCLE
        );
        newStudentPackage.setCycleNo(
                studentPackageRepository.findMaxCycleNoByEnrollmentId(oldStudentPackage.getEnrollment().getId()) + 1
        );
        return newStudentPackage;
    }

    private Invoice createInvoice(
            StudentPackage oldStudentPackage,
            StudentPackage newStudentPackage,
            TuitionPackage newTuitionPackage,
            BigDecimal amountToPay,
            LocalDate changeDate,
            String reason
    ) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceCode(generateInvoiceCode());
        invoice.setStudent(oldStudentPackage.getStudent());
        invoice.setClassroom(oldStudentPackage.getClassroom());
        invoice.setEnrollment(oldStudentPackage.getEnrollment());
        invoice.setStudentPackage(newStudentPackage);
        invoice.setPackageNameSnapshot(newTuitionPackage.getName());
        invoice.setTotalSessionsSnapshot(newTuitionPackage.getTotalSessions());
        invoice.setAmount(newTuitionPackage.getPrice());
        invoice.setDiscountAmount(ZERO);
        invoice.setAdjustmentAmount(money(amountToPay.subtract(newTuitionPackage.getPrice())));
        invoice.setFinalAmount(amountToPay);
        invoice.setPaidAmount(ZERO);
        invoice.setRemainingAmount(amountToPay);
        invoice.setDueDate(changeDate);
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setNote("Đổi gói: " + reason);
        return invoice;
    }

    private PackageChangeLog createLog(
            StudentPackage oldStudentPackage,
            StudentPackage newStudentPackage,
            ChangePackagePreviewResponse calculation,
            String reason
    ) {
        PackageChangeLog log = new PackageChangeLog();
        log.setStudent(oldStudentPackage.getStudent());
        log.setClassroom(oldStudentPackage.getClassroom());
        log.setOldStudentPackage(oldStudentPackage);
        log.setNewStudentPackage(newStudentPackage);
        log.setOldPackageName(calculation.oldPackageName());
        log.setNewPackageName(calculation.newPackageName());
        log.setOldTotalSessions(calculation.oldTotalSessions());
        log.setNewTotalSessions(calculation.newTotalSessions());
        log.setOldFinalAmount(calculation.oldFinalAmount());
        log.setNewPackagePrice(calculation.newPackagePrice());
        log.setUsedSessions(calculation.usedSessions());
        log.setOldUnitPrice(calculation.oldUnitPrice());
        log.setUsedAmount(calculation.usedAmount());
        log.setPaidAmount(calculation.totalValidPaidAmount());
        log.setAdjustmentAmount(calculation.adjustmentAmount());
        log.setAdjustmentType(calculation.adjustmentType());
        log.setNewInvoiceFinalAmount(calculation.amountToPay());
        log.setReason(reason);
        return log;
    }

    private StudentPackage findStudentPackage(Long id) {
        return studentPackageRepository.findWithRelationsById(id)
                .orElseThrow(() -> new NotFoundException("Student package not found"));
    }

    private StudentPackage findStudentPackageForUpdate(Long id) {
        return studentPackageRepository.findWithRelationsForUpdateById(id)
                .orElseThrow(() -> new NotFoundException("Student package not found"));
    }

    private TuitionPackage findTuitionPackage(Long id) {
        return tuitionPackageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tuition package not found"));
    }

    private void validateChangeAllowed(StudentPackage oldStudentPackage, TuitionPackage newTuitionPackage) {
        Enrollment enrollment = oldStudentPackage.getEnrollment();
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BusinessException("Enrollment must be active to change package");
        }

        StudentPackage latestPackage = studentPackageRepository
                .findTopByEnrollmentIdOrderByCycleNoDescIdDesc(enrollment.getId())
                .orElseThrow(() -> new BusinessException("Student package not found"));
        if (!latestPackage.getId().equals(oldStudentPackage.getId())) {
            throw new BusinessException("Only the latest student package can be changed");
        }

        if (newTuitionPackage.getStatus() != TuitionPackageStatus.ACTIVE) {
            throw new BusinessException("New tuition package must be active");
        }

        if (oldStudentPackage.getTuitionPackage().getId().equals(newTuitionPackage.getId())) {
            throw new BusinessException("New tuition package must be different from current package");
        }

        boolean linkedToClassroom = classPackageRepository.existsByClassroomIdAndTuitionPackageIdAndActiveTrue(
                oldStudentPackage.getClassroom().getId(),
                newTuitionPackage.getId()
        );
        if (!linkedToClassroom) {
            throw new BusinessException("Tuition package is not linked to this classroom");
        }
    }

    private void validateNoExistingPackageChange(Long oldStudentPackageId) {
        if (packageChangeLogRepository.existsByOldStudentPackageId(oldStudentPackageId)) {
            throw new BusinessException("Student package has already been changed");
        }
    }

    private List<PackageChangeMode> allowedModes() {
        return List.of(PackageChangeMode.REPLACEMENT_CHANGE, PackageChangeMode.NEW_CYCLE_CHANGE);
    }

    private void validateModeAllowed(
            PackageChangeMode changeMode,
            int usedSessions,
            TuitionPackage newTuitionPackage
    ) {
        if (changeMode == PackageChangeMode.REPLACEMENT_CHANGE
                && usedSessions > newTuitionPackage.getTotalSessions()) {
            throw new BusinessException("Used sessions exceed new package total sessions; replacement change is not allowed");
        }
    }

    private PackageChangeAdjustmentType resolveAdjustmentType(BigDecimal adjustmentAmount) {
        if (adjustmentAmount.compareTo(ZERO) > 0) {
            return PackageChangeAdjustmentType.CREDIT;
        }

        if (adjustmentAmount.compareTo(ZERO) < 0) {
            return PackageChangeAdjustmentType.DEBT;
        }

        return PackageChangeAdjustmentType.NONE;
    }

    private BigDecimal divideMoney(BigDecimal amount, BigDecimal divisor) {
        return money(amount.divide(divisor, 2, RoundingMode.HALF_UP));
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount == null) {
            return ZERO;
        }

        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String generateInvoiceCode() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
