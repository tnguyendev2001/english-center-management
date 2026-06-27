package com.englishcenter.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishcenter.classpackage.ClassPackageRepository;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.enrollment.dto.EnrollStudentRequest;
import com.englishcenter.enrollment.dto.EnrollmentResponse;
import com.englishcenter.enrollment.mapper.EnrollmentMapper;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.invoice.mapper.InvoiceMapper;
import com.englishcenter.student.Student;
import com.englishcenter.student.StudentRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {
    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private TuitionPackageRepository tuitionPackageRepository;

    @Mock
    private ClassPackageRepository classPackageRepository;

    @Mock
    private StudentPackageRepository studentPackageRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    private final EnrollmentMapper enrollmentMapper = new EnrollmentMapper(
            new StudentPackageMapper(),
            new InvoiceMapper()
    );

    @Test
    void enrollStudentCreatesEnrollmentStudentPackageAndInvoice() {
        EnrollmentService service = newService();
        mockValidLookups(false, true);
        mockSaves();

        EnrollmentResponse response = service.enrollStudent(validRequest());

        assertThat(response.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(response.studentPackage()).isNotNull();
        assertThat(response.studentPackage().status()).isEqualTo(StudentPackageStatus.ACTIVE);
        assertThat(response.invoice()).isNotNull();
        assertThat(response.invoice().status()).isEqualTo(InvoiceStatus.UNPAID);
        verify(enrollmentRepository).save(any(Enrollment.class));
        verify(studentPackageRepository).save(any(StudentPackage.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void enrollStudentRejectsDuplicateActiveEnrollment() {
        EnrollmentService service = newService();
        mockValidLookups(true, true);

        assertThatThrownBy(() -> service.enrollStudent(validRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Student already has an active enrollment in this classroom");

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
        verify(studentPackageRepository, never()).save(any(StudentPackage.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void enrollStudentRejectsPackageNotLinkedToClassroom() {
        EnrollmentService service = newService();
        mockValidLookups(false, false);

        assertThatThrownBy(() -> service.enrollStudent(validRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tuition package is not linked to this classroom");

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
        verify(studentPackageRepository, never()).save(any(StudentPackage.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void enrollStudentStoresPackageSnapshotInInvoice() {
        EnrollmentService service = newService();
        mockValidLookups(false, true);
        mockSaves();

        service.enrollStudent(validRequest());

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice invoice = invoiceCaptor.getValue();

        assertThat(invoice.getPackageNameSnapshot()).isEqualTo("8 sessions");
        assertThat(invoice.getTotalSessionsSnapshot()).isEqualTo(8);
        assertThat(invoice.getAmount()).isEqualByComparingTo("500000");
        assertThat(invoice.getDiscountAmount()).isEqualByComparingTo("50000");
        assertThat(invoice.getFinalAmount()).isEqualByComparingTo("450000");
    }

    @Test
    void enrollStudentCreatesUnpaidInvoiceWithZeroPaidAndRemainingFinalAmount() {
        EnrollmentService service = newService();
        mockValidLookups(false, true);
        mockSaves();

        service.enrollStudent(validRequest());

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice invoice = invoiceCaptor.getValue();

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        assertThat(invoice.getPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(invoice.getRemainingAmount()).isEqualByComparingTo(invoice.getFinalAmount());
    }

    private EnrollmentService newService() {
        return new EnrollmentService(
                enrollmentRepository,
                studentRepository,
                classroomRepository,
                tuitionPackageRepository,
                classPackageRepository,
                studentPackageRepository,
                invoiceRepository,
                enrollmentMapper
        );
    }

    private void mockValidLookups(boolean hasDuplicateEnrollment, boolean packageLinkedToClassroom) {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student()));
        when(classroomRepository.findById(2L)).thenReturn(Optional.of(classroom()));
        when(tuitionPackageRepository.findById(3L)).thenReturn(Optional.of(tuitionPackage()));
        when(classPackageRepository.existsByClassroomIdAndTuitionPackageIdAndActiveTrue(2L, 3L))
                .thenReturn(packageLinkedToClassroom);

        if (packageLinkedToClassroom) {
            when(enrollmentRepository.existsByStudentIdAndClassroomIdAndStatus(
                    1L,
                    2L,
                    EnrollmentStatus.ACTIVE
            )).thenReturn(hasDuplicateEnrollment);
        }
    }

    private void mockSaves() {
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            enrollment.setId(10L);
            enrollment.setCreatedAt(LocalDateTime.now());
            enrollment.setUpdatedAt(LocalDateTime.now());
            return enrollment;
        });
        when(studentPackageRepository.save(any(StudentPackage.class))).thenAnswer(invocation -> {
            StudentPackage studentPackage = invocation.getArgument(0);
            studentPackage.setId(20L);
            studentPackage.setCreatedAt(LocalDateTime.now());
            studentPackage.setUpdatedAt(LocalDateTime.now());
            return studentPackage;
        });
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            invoice.setId(30L);
            invoice.setCreatedAt(LocalDateTime.now());
            invoice.setUpdatedAt(LocalDateTime.now());
            return invoice;
        });
    }

    private EnrollStudentRequest validRequest() {
        return new EnrollStudentRequest(
                1L,
                2L,
                3L,
                LocalDate.of(2026, 7, 1),
                new BigDecimal("50000"),
                "First enrollment"
        );
    }

    private Student student() {
        Student student = new Student();
        student.setId(1L);
        student.setStudentCode("STU001");
        student.setFullName("Nguyen Van A");
        return student;
    }

    private Classroom classroom() {
        Classroom classroom = new Classroom();
        classroom.setId(2L);
        classroom.setClassCode("CLS001");
        classroom.setClassName("Starter A");
        return classroom;
    }

    private TuitionPackage tuitionPackage() {
        TuitionPackage tuitionPackage = new TuitionPackage();
        tuitionPackage.setId(3L);
        tuitionPackage.setName("8 sessions");
        tuitionPackage.setTotalSessions(8);
        tuitionPackage.setPrice(new BigDecimal("500000"));
        tuitionPackage.setStatus(TuitionPackageStatus.ACTIVE);
        return tuitionPackage;
    }
}
