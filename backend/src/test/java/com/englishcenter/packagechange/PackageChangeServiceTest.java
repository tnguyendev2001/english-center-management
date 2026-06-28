package com.englishcenter.packagechange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.classpackage.ClassPackageRepository;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.invoice.mapper.InvoiceMapper;
import com.englishcenter.makeupcredit.MakeupCreditRepository;
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import com.englishcenter.packagechange.dto.ChangePackageRequest;
import com.englishcenter.packagechange.dto.ChangePackageResponse;
import com.englishcenter.packagechange.dto.ChangePackagePreviewResponse;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.student.Student;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PackageChangeServiceTest {
    @Mock
    private StudentPackageRepository studentPackageRepository;

    @Mock
    private TuitionPackageRepository tuitionPackageRepository;

    @Mock
    private ClassPackageRepository classPackageRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private MakeupCreditRepository makeupCreditRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PackageChangeLogRepository packageChangeLogRepository;

    private final StudentPackageMapper studentPackageMapper = new StudentPackageMapper();
    private final InvoiceMapper invoiceMapper = new InvoiceMapper();

    @Test
    void previewDoesNotCreateInvoiceStudentPackageOrLog() {
        PackageChangeService service = newService();
        StudentPackage oldPackage = oldStudentPackage();
        TuitionPackage newPackage = tuitionPackage(4L, "16 sessions", 16, "900000");

        when(studentPackageRepository.findWithRelationsById(20L)).thenReturn(Optional.of(oldPackage));
        when(tuitionPackageRepository.findById(4L)).thenReturn(Optional.of(newPackage));
        when(classPackageRepository.existsByClassroomIdAndTuitionPackageIdAndActiveTrue(2L, 4L))
                .thenReturn(true);
        when(attendanceRepository.countUsedSessions(
                1L,
                2L,
                oldPackage.getStartDate(),
                oldPackage.getEndDate(),
                ClassSessionStatus.CANCELED
        )).thenReturn(3L);
        when(makeupCreditRepository.sumAvailableMakeupSessions(1L, 2L, MakeupCreditStatus.AVAILABLE))
                .thenReturn(1);
        when(paymentRepository.sumValidAmountByStudentPackageId(20L)).thenReturn(new BigDecimal("500000"));

        ChangePackagePreviewResponse response = service.preview(20L, 4L, PackageChangeMode.NEW_CYCLE_CHANGE);

        assertThat(response.newInvoiceFinalAmount()).isEqualByComparingTo("587500.00");
        verify(studentPackageRepository, never()).save(any(StudentPackage.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(packageChangeLogRepository, never()).save(any(PackageChangeLog.class));
    }

    @Test
    void changePackageWithCreditClosesOldPackageCreatesNewPackageAndAdjustedInvoice() {
        PackageChangeService service = newService();
        StudentPackage oldPackage = oldStudentPackage();
        TuitionPackage newPackage = tuitionPackage(4L, "16 sessions", 16, "900000");
        Invoice oldInvoice = oldInvoice(oldPackage);
        mockValidChange(oldPackage, newPackage, 3L, new BigDecimal("500000"));
        mockSaves();

        ChangePackageResponse response = service.changePackage(
                20L,
                new ChangePackageRequest(4L, PackageChangeMode.NEW_CYCLE_CHANGE, "Student changes plan")
        );

        assertThat(response.calculation().adjustmentType()).isEqualTo(PackageChangeAdjustmentType.CREDIT);
        assertThat(response.calculation().oldUnitPrice()).isEqualByComparingTo("62500.00");
        assertThat(response.calculation().usedAmount()).isEqualByComparingTo("187500.00");
        assertThat(response.calculation().adjustmentAmount()).isEqualByComparingTo("312500.00");
        assertThat(response.calculation().newInvoiceAdjustmentAmount()).isEqualByComparingTo("-312500.00");
        assertThat(response.calculation().newInvoiceFinalAmount()).isEqualByComparingTo("587500.00");
        assertThat(oldPackage.getStatus()).isEqualTo(StudentPackageStatus.CLOSED);
        assertThat(response.newStudentPackage().status()).isEqualTo(StudentPackageStatus.ACTIVE);
        assertThat(response.newInvoice().adjustmentAmount()).isEqualByComparingTo("-312500.00");
        assertThat(response.newInvoice().finalAmount()).isEqualByComparingTo("587500.00");
        assertOldInvoiceUnchanged(oldInvoice);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
        verify(invoiceRepository).replaceCollectibleInvoicesByStudentPackageId(any(), any());
        verify(packageChangeLogRepository).save(any(PackageChangeLog.class));
    }

    @Test
    void changePackageWithDebtCreatesPositiveInvoiceAdjustment() {
        PackageChangeService service = newService();
        StudentPackage oldPackage = oldStudentPackage();
        TuitionPackage newPackage = tuitionPackage(4L, "12 sessions", 12, "700000");
        mockValidChange(oldPackage, newPackage, 4L, new BigDecimal("200000"));
        mockSaves();

        ChangePackageResponse response = service.changePackage(
                20L,
                new ChangePackageRequest(4L, PackageChangeMode.NEW_CYCLE_CHANGE, "Need fewer sessions")
        );

        assertThat(response.calculation().adjustmentType()).isEqualTo(PackageChangeAdjustmentType.DEBT);
        assertThat(response.calculation().usedAmount()).isEqualByComparingTo("250000.00");
        assertThat(response.calculation().adjustmentAmount()).isEqualByComparingTo("-50000.00");
        assertThat(response.newInvoice().adjustmentAmount()).isEqualByComparingTo("50000.00");
        assertThat(response.newInvoice().finalAmount()).isEqualByComparingTo("750000.00");
    }

    @Test
    void changePackageFromTwelveToThirtySixWithFullPaymentAndNoUsedSessionsChargesDifferenceOnly() {
        PackageChangeService service = newService();
        StudentPackage oldPackage = oldStudentPackage(tuitionPackage(3L, "12 sessions", 12, "700000"));
        TuitionPackage newPackage = tuitionPackage(4L, "36 sessions", 36, "2000000");
        mockValidChange(oldPackage, newPackage, 0L, new BigDecimal("700000"));
        mockSaves();

        ChangePackageResponse response = service.changePackage(
                20L,
                new ChangePackageRequest(4L, PackageChangeMode.REPLACEMENT_CHANGE, "Upgrade package")
        );

        assertThat(response.calculation().usedAmount()).isEqualByComparingTo("0.00");
        assertThat(response.calculation().changeMode()).isEqualTo(PackageChangeMode.REPLACEMENT_CHANGE);
        assertThat(response.calculation().amountToPay()).isEqualByComparingTo("1300000.00");
        assertThat(response.calculation().remainingSessionsAfterChange()).isEqualTo(36);
        assertThat(response.newInvoice().finalAmount()).isEqualByComparingTo("1300000.00");
        assertThat(response.newStudentPackage().totalSessions()).isEqualTo(36);
        assertThat(response.newStudentPackage().price()).isEqualByComparingTo("2000000");
    }

    @Test
    void replacementChangeFromTwelveToThirtySixWithFullPaymentAndOneUsedSessionChargesDifferenceOnly() {
        PackageChangeService service = newService();
        StudentPackage oldPackage = oldStudentPackage(tuitionPackage(3L, "12 sessions", 12, "700000"));
        TuitionPackage newPackage = tuitionPackage(4L, "36 sessions", 36, "2000000");
        mockValidChange(oldPackage, newPackage, 1L, new BigDecimal("700000"));
        mockSaves();

        ChangePackageResponse response = service.changePackage(
                20L,
                new ChangePackageRequest(4L, PackageChangeMode.REPLACEMENT_CHANGE, "Upgrade package")
        );

        assertThat(response.calculation().changeMode()).isEqualTo(PackageChangeMode.REPLACEMENT_CHANGE);
        assertThat(response.calculation().amountToPay()).isEqualByComparingTo("1300000.00");
        assertThat(response.calculation().remainingSessionsAfterChange()).isEqualTo(35);
        assertThat(response.newInvoice().finalAmount()).isEqualByComparingTo("1300000.00");
    }

    @Test
    void newCycleChangeFromTwelveToThirtySixWithFullPaymentAndNineUsedSessionsUsesUnusedCredit() {
        PackageChangeService service = newService();
        StudentPackage oldPackage = oldStudentPackage(tuitionPackage(3L, "12 sessions", 12, "700000"));
        TuitionPackage newPackage = tuitionPackage(4L, "36 sessions", 36, "2000000");
        mockValidChange(oldPackage, newPackage, 9L, new BigDecimal("700000"));
        mockSaves();

        ChangePackageResponse response = service.changePackage(
                20L,
                new ChangePackageRequest(4L, PackageChangeMode.NEW_CYCLE_CHANGE, "Upgrade package")
        );

        assertThat(response.calculation().usedAmount()).isEqualByComparingTo("525000.00");
        assertThat(response.calculation().unusedCredit()).isEqualByComparingTo("175000.00");
        assertThat(response.calculation().amountToPay()).isEqualByComparingTo("1825000.00");
        assertThat(response.calculation().remainingSessionsAfterChange()).isEqualTo(36);
        assertThat(response.newInvoice().finalAmount()).isEqualByComparingTo("1825000.00");
    }

    @Test
    void newCycleChangeFromTwelveToThirtySixWithNoPaymentAndOneUsedSessionAddsDebt() {
        PackageChangeService service = newService();
        StudentPackage oldPackage = oldStudentPackage(tuitionPackage(3L, "12 sessions", 12, "700000"));
        TuitionPackage newPackage = tuitionPackage(4L, "36 sessions", 36, "2000000");
        mockValidChange(oldPackage, newPackage, 1L, BigDecimal.ZERO);
        mockSaves();

        ChangePackageResponse response = service.changePackage(
                20L,
                new ChangePackageRequest(4L, PackageChangeMode.NEW_CYCLE_CHANGE, "Upgrade package")
        );

        assertThat(response.calculation().usedAmount()).isEqualByComparingTo("58333.33");
        assertThat(response.calculation().oldDebt()).isEqualByComparingTo("58333.33");
        assertThat(response.calculation().amountToPay()).isEqualByComparingTo("2058333.33");
        assertThat(response.newInvoice().finalAmount()).isEqualByComparingTo("2058333.33");
    }

    @Test
    void replacementChangeIsRejectedWhenUsedSessionsExceedNewPackageTotal() {
        PackageChangeService service = newService();
        StudentPackage oldPackage = oldStudentPackage(tuitionPackage(3L, "12 sessions", 12, "700000"));
        TuitionPackage newPackage = tuitionPackage(4L, "8 sessions", 8, "500000");
        when(studentPackageRepository.findWithRelationsForUpdateById(20L)).thenReturn(Optional.of(oldPackage));
        when(tuitionPackageRepository.findById(4L)).thenReturn(Optional.of(newPackage));
        when(classPackageRepository.existsByClassroomIdAndTuitionPackageIdAndActiveTrue(2L, 4L))
                .thenReturn(true);
        when(attendanceRepository.countUsedSessions(
                1L,
                2L,
                oldPackage.getStartDate(),
                oldPackage.getEndDate(),
                ClassSessionStatus.CANCELED
        )).thenReturn(9L);

        assertThatThrownBy(() -> service.changePackage(
                20L,
                new ChangePackageRequest(4L, PackageChangeMode.REPLACEMENT_CHANGE, "Invalid replacement")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Used sessions exceed new package total sessions; replacement change is not allowed");

        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(packageChangeLogRepository, never()).save(any(PackageChangeLog.class));
    }

    @Test
    void replacementChangeIsAllowedWhenAdminSelectsItDespiteManyUsedSessions() {
        PackageChangeService service = newService();
        StudentPackage oldPackage = oldStudentPackage(tuitionPackage(3L, "12 sessions", 12, "700000"));
        TuitionPackage newPackage = tuitionPackage(4L, "36 sessions", 36, "2000000");
        mockValidChange(oldPackage, newPackage, 9L, new BigDecimal("700000"));
        mockSaves();

        ChangePackageResponse response = service.changePackage(
                20L,
                new ChangePackageRequest(4L, PackageChangeMode.REPLACEMENT_CHANGE, "Admin override")
        );

        assertThat(response.calculation().remainingSessionsAfterChange()).isEqualTo(27);
        assertThat(response.calculation().amountToPay()).isEqualByComparingTo("1300000.00");
    }

    @Test
    void changePackageFromThirtySixToTwelveCreatesActiveTwelveSessionPackage() {
        PackageChangeService service = newService();
        StudentPackage oldPackage = oldStudentPackage(tuitionPackage(3L, "36 sessions", 36, "2000000"));
        TuitionPackage newPackage = tuitionPackage(4L, "12 sessions", 12, "700000");
        mockValidChange(oldPackage, newPackage, 0L, BigDecimal.ZERO);
        mockSaves();

        ChangePackageResponse response = service.changePackage(
                20L,
                new ChangePackageRequest(4L, PackageChangeMode.NEW_CYCLE_CHANGE, "Downsize package")
        );

        assertThat(oldPackage.getStatus()).isEqualTo(StudentPackageStatus.CLOSED);
        assertThat(response.newStudentPackage().status()).isEqualTo(StudentPackageStatus.ACTIVE);
        assertThat(response.newStudentPackage().totalSessions()).isEqualTo(12);
        assertThat(response.newStudentPackage().packageName()).isEqualTo("12 sessions");
        assertThat(response.newInvoice().packageNameSnapshot()).isEqualTo("12 sessions");
        assertThat(response.newInvoice().amount()).isEqualByComparingTo("700000");
    }

    @Test
    void changePackageReplacesOldUnpaidOrPartiallyPaidInvoice() {
        PackageChangeService service = newService();
        StudentPackage oldPackage = oldStudentPackage();
        TuitionPackage newPackage = tuitionPackage(4L, "12 sessions", 12, "700000");
        mockValidChange(oldPackage, newPackage, 4L, new BigDecimal("200000"));
        mockSaves();

        service.changePackage(
                20L,
                new ChangePackageRequest(4L, PackageChangeMode.NEW_CYCLE_CHANGE, "Need fewer sessions")
        );

        verify(invoiceRepository).replaceCollectibleInvoicesByStudentPackageId(any(), any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void changePackageRejectsRepeatedConfirmForSameOldPackage() {
        PackageChangeService service = newService();
        StudentPackage oldPackage = oldStudentPackage();
        TuitionPackage newPackage = tuitionPackage(4L, "12 sessions", 12, "700000");

        when(studentPackageRepository.findWithRelationsForUpdateById(20L)).thenReturn(Optional.of(oldPackage));
        when(tuitionPackageRepository.findById(4L)).thenReturn(Optional.of(newPackage));
        when(classPackageRepository.existsByClassroomIdAndTuitionPackageIdAndActiveTrue(2L, 4L))
                .thenReturn(true);
        when(packageChangeLogRepository.existsByOldStudentPackageId(20L)).thenReturn(true);

        assertThatThrownBy(() -> service.changePackage(
                20L,
                new ChangePackageRequest(4L, PackageChangeMode.NEW_CYCLE_CHANGE, "Duplicate click")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Student package has already been changed");

        verify(studentPackageRepository, never()).save(any(StudentPackage.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(packageChangeLogRepository, never()).save(any(PackageChangeLog.class));
    }

    @Test
    void newCycleChangeCarriesUnusedCreditWhenDowngradingWithFullPayment() {
        PackageChangeService service = newService();
        StudentPackage oldPackage = oldStudentPackage(tuitionPackage(3L, "8 sessions", 8, "800000"));
        TuitionPackage newPackage = tuitionPackage(4L, "4 sessions", 4, "400000");
        mockValidChange(oldPackage, newPackage, 4L, new BigDecimal("800000"));
        when(studentPackageRepository.findWithRelationsById(20L)).thenReturn(Optional.of(oldPackage));
        mockPackageChangeSaves();

        ChangePackagePreviewResponse preview = service.preview(
                20L,
                4L,
                PackageChangeMode.NEW_CYCLE_CHANGE
        );

        assertThat(preview.totalValidPaidAmount()).isEqualByComparingTo("800000.00");
        assertThat(preview.usedAmount()).isEqualByComparingTo("400000.00");
        assertThat(preview.unusedCredit()).isEqualByComparingTo("400000.00");
        assertThat(preview.amountToPay()).isEqualByComparingTo("0.00");
        assertThat(preview.collectibleAmount()).isEqualByComparingTo("0.00");

        ChangePackageResponse response = service.changePackage(
                20L,
                new ChangePackageRequest(4L, PackageChangeMode.NEW_CYCLE_CHANGE, "Downgrade package")
        );

        assertThat(response.newInvoice()).isNull();
    }

    @Test
    void newCycleChangeAppliesPartialUnusedCreditWhenDowngradingWithFiveUsedSessions() {
        PackageChangeService service = newService();
        StudentPackage oldPackage = oldStudentPackage(tuitionPackage(3L, "8 sessions", 8, "800000"));
        TuitionPackage newPackage = tuitionPackage(4L, "4 sessions", 4, "400000");
        mockValidPreview(oldPackage, newPackage, 5L, new BigDecimal("800000"));

        ChangePackagePreviewResponse preview = service.preview(
                20L,
                4L,
                PackageChangeMode.NEW_CYCLE_CHANGE
        );

        assertThat(preview.usedAmount()).isEqualByComparingTo("500000.00");
        assertThat(preview.unusedCredit()).isEqualByComparingTo("300000.00");
        assertThat(preview.amountToPay()).isEqualByComparingTo("100000.00");
        assertThat(preview.collectibleAmount()).isEqualByComparingTo("100000.00");
    }

    @Test
    void newCycleChangeAddsOldDebtWhenStudentUnderpaidOldPackage() {
        PackageChangeService service = newService();
        StudentPackage oldPackage = oldStudentPackage(tuitionPackage(3L, "8 sessions", 8, "800000"));
        TuitionPackage newPackage = tuitionPackage(4L, "4 sessions", 4, "400000");
        mockValidPreview(oldPackage, newPackage, 5L, new BigDecimal("400000"));

        ChangePackagePreviewResponse preview = service.preview(
                20L,
                4L,
                PackageChangeMode.NEW_CYCLE_CHANGE
        );

        assertThat(preview.usedAmount()).isEqualByComparingTo("500000.00");
        assertThat(preview.oldDebt()).isEqualByComparingTo("100000.00");
        assertThat(preview.amountToPay()).isEqualByComparingTo("500000.00");
    }

    @Test
    void newCycleChangeRequiresFullNewPackagePriceWhenOldPackageFullyUsed() {
        PackageChangeService service = newService();
        StudentPackage oldPackage = oldStudentPackage(tuitionPackage(3L, "8 sessions", 8, "800000"));
        TuitionPackage newPackage = tuitionPackage(4L, "4 sessions", 4, "400000");
        mockValidPreview(oldPackage, newPackage, 8L, new BigDecimal("800000"));

        ChangePackagePreviewResponse preview = service.preview(
                20L,
                4L,
                PackageChangeMode.NEW_CYCLE_CHANGE
        );

        assertThat(preview.unusedCredit()).isEqualByComparingTo("0.00");
        assertThat(preview.amountToPay()).isEqualByComparingTo("400000.00");
    }

    @Test
    void changePackageRejectsPackageNotLinkedToClassroom() {
        PackageChangeService service = newService();
        StudentPackage oldPackage = oldStudentPackage();
        TuitionPackage newPackage = tuitionPackage(4L, "12 sessions", 12, "700000");

        when(studentPackageRepository.findWithRelationsForUpdateById(20L)).thenReturn(Optional.of(oldPackage));
        when(tuitionPackageRepository.findById(4L)).thenReturn(Optional.of(newPackage));
        when(classPackageRepository.existsByClassroomIdAndTuitionPackageIdAndActiveTrue(2L, 4L))
                .thenReturn(false);

        assertThatThrownBy(() -> service.changePackage(
                20L,
                new ChangePackageRequest(4L, PackageChangeMode.NEW_CYCLE_CHANGE, "Need fewer sessions")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tuition package is not linked to this classroom");

        verify(studentPackageRepository, never()).save(any(StudentPackage.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(packageChangeLogRepository, never()).save(any(PackageChangeLog.class));
    }

    private PackageChangeService newService() {
        return new PackageChangeService(
                studentPackageRepository,
                tuitionPackageRepository,
                classPackageRepository,
                attendanceRepository,
                makeupCreditRepository,
                paymentRepository,
                invoiceRepository,
                packageChangeLogRepository,
                studentPackageMapper,
                invoiceMapper
        );
    }

    private void mockValidChange(
            StudentPackage oldPackage,
            TuitionPackage newPackage,
            long usedSessions,
            BigDecimal paidAmount
    ) {
        when(studentPackageRepository.findWithRelationsForUpdateById(20L)).thenReturn(Optional.of(oldPackage));
        when(tuitionPackageRepository.findById(newPackage.getId())).thenReturn(Optional.of(newPackage));
        when(classPackageRepository.existsByClassroomIdAndTuitionPackageIdAndActiveTrue(2L, newPackage.getId()))
                .thenReturn(true);
        when(attendanceRepository.countUsedSessions(
                1L,
                2L,
                oldPackage.getStartDate(),
                oldPackage.getEndDate(),
                ClassSessionStatus.CANCELED
        )).thenReturn(usedSessions);
        when(makeupCreditRepository.sumAvailableMakeupSessions(1L, 2L, MakeupCreditStatus.AVAILABLE))
                .thenReturn(1);
        when(paymentRepository.sumValidAmountByStudentPackageId(20L)).thenReturn(paidAmount);
        when(studentPackageRepository.findMaxCycleNoByEnrollmentId(10L)).thenReturn(1);
    }

    private void mockValidPreview(
            StudentPackage oldPackage,
            TuitionPackage newPackage,
            long usedSessions,
            BigDecimal paidAmount
    ) {
        when(studentPackageRepository.findWithRelationsById(20L)).thenReturn(Optional.of(oldPackage));
        when(tuitionPackageRepository.findById(newPackage.getId())).thenReturn(Optional.of(newPackage));
        when(classPackageRepository.existsByClassroomIdAndTuitionPackageIdAndActiveTrue(2L, newPackage.getId()))
                .thenReturn(true);
        when(attendanceRepository.countUsedSessions(
                1L,
                2L,
                oldPackage.getStartDate(),
                oldPackage.getEndDate(),
                ClassSessionStatus.CANCELED
        )).thenReturn(usedSessions);
        when(makeupCreditRepository.sumAvailableMakeupSessions(1L, 2L, MakeupCreditStatus.AVAILABLE))
                .thenReturn(1);
        when(paymentRepository.sumValidAmountByStudentPackageId(20L)).thenReturn(paidAmount);
    }

    private void mockPackageChangeSaves() {
        when(studentPackageRepository.save(any(StudentPackage.class))).thenAnswer(invocation -> {
            StudentPackage studentPackage = invocation.getArgument(0);
            if (studentPackage.getId() == null) {
                studentPackage.setId(21L);
                studentPackage.setCreatedAt(LocalDateTime.now());
                studentPackage.setUpdatedAt(LocalDateTime.now());
            }
            return studentPackage;
        });
        when(packageChangeLogRepository.save(any(PackageChangeLog.class))).thenAnswer(invocation -> {
            PackageChangeLog log = invocation.getArgument(0);
            log.setId(41L);
            return log;
        });
    }

    private void mockSaves() {
        mockPackageChangeSaves();
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            invoice.setId(31L);
            invoice.setCreatedAt(LocalDateTime.now());
            invoice.setUpdatedAt(LocalDateTime.now());
            return invoice;
        });
    }

    private StudentPackage oldStudentPackage() {
        return oldStudentPackage(tuitionPackage(3L, "8 sessions", 8, "500000"));
    }

    private StudentPackage oldStudentPackage(TuitionPackage tuitionPackage) {
        Student student = new Student();
        student.setId(1L);
        student.setFullName("Nguyen Van A");

        Classroom classroom = new Classroom();
        classroom.setId(2L);
        classroom.setClassName("Starter A");

        Enrollment enrollment = new Enrollment();
        enrollment.setId(10L);
        enrollment.setStudent(student);
        enrollment.setClassroom(classroom);
        enrollment.setStartDate(LocalDate.of(2026, 7, 1));

        StudentPackage studentPackage = new StudentPackage();
        studentPackage.setId(20L);
        studentPackage.setStudent(student);
        studentPackage.setClassroom(classroom);
        studentPackage.setEnrollment(enrollment);
        studentPackage.setTuitionPackage(tuitionPackage);
        studentPackage.setPackageName(tuitionPackage.getName());
        studentPackage.setTotalSessions(tuitionPackage.getTotalSessions());
        studentPackage.setPrice(tuitionPackage.getPrice());
        studentPackage.setDiscountAmount(BigDecimal.ZERO);
        studentPackage.setAdjustmentAmount(BigDecimal.ZERO);
        studentPackage.setFinalAmount(tuitionPackage.getPrice());
        studentPackage.setStartDate(LocalDate.of(2026, 7, 1));
        studentPackage.setStatus(StudentPackageStatus.ACTIVE);
        studentPackage.setCycleNo(1);
        return studentPackage;
    }

    private TuitionPackage tuitionPackage(Long id, String name, int totalSessions, String price) {
        TuitionPackage tuitionPackage = new TuitionPackage();
        tuitionPackage.setId(id);
        tuitionPackage.setName(name);
        tuitionPackage.setTotalSessions(totalSessions);
        tuitionPackage.setPrice(new BigDecimal(price));
        tuitionPackage.setStatus(TuitionPackageStatus.ACTIVE);
        return tuitionPackage;
    }

    private Invoice oldInvoice(StudentPackage oldPackage) {
        Invoice invoice = new Invoice();
        invoice.setId(30L);
        invoice.setInvoiceCode("INV-OLD");
        invoice.setStudent(oldPackage.getStudent());
        invoice.setClassroom(oldPackage.getClassroom());
        invoice.setEnrollment(oldPackage.getEnrollment());
        invoice.setStudentPackage(oldPackage);
        invoice.setPackageNameSnapshot(oldPackage.getPackageName());
        invoice.setTotalSessionsSnapshot(oldPackage.getTotalSessions());
        invoice.setAmount(oldPackage.getPrice());
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setAdjustmentAmount(BigDecimal.ZERO);
        invoice.setFinalAmount(oldPackage.getFinalAmount());
        invoice.setPaidAmount(new BigDecimal("500000"));
        invoice.setRemainingAmount(BigDecimal.ZERO);
        invoice.setStatus(InvoiceStatus.PAID);
        return invoice;
    }

    private void assertOldInvoiceUnchanged(Invoice oldInvoice) {
        assertThat(oldInvoice.getPackageNameSnapshot()).isEqualTo("8 sessions");
        assertThat(oldInvoice.getAmount()).isEqualByComparingTo("500000");
        assertThat(oldInvoice.getAdjustmentAmount()).isEqualByComparingTo("0");
        assertThat(oldInvoice.getFinalAmount()).isEqualByComparingTo("500000");
        assertThat(oldInvoice.getPaidAmount()).isEqualByComparingTo("500000");
        assertThat(oldInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }
}
