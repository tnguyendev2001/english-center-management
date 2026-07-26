package com.englishcenter.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.classpackage.ClassPackageRepository;
import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classroom.ClassroomStatus;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.enrollment.mapper.EnrollmentMapper;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.invoice.mapper.InvoiceMapper;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.student.Student;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.student.StudentStatus;
import com.englishcenter.student.mapper.StudentMapper;
import com.englishcenter.studentpackage.StudentPackage;
import com.englishcenter.studentpackage.StudentPackageRepository;
import com.englishcenter.studentpackage.StudentPackageSourceType;
import com.englishcenter.studentpackage.mapper.StudentPackageMapper;
import com.englishcenter.tuitionpackage.TuitionPackage;
import com.englishcenter.tuitionpackage.TuitionPackageRepository;
import com.englishcenter.tuitionpackage.TuitionPackageStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentLegacyImportTest {
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private EnrollmentStatusHistoryRepository statusHistoryRepository;
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
    @Mock
    private ClassSessionRepository classSessionRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private PaymentRepository paymentRepository;

    @Test
    void enrollFromLegacyImportCreatesUnpaidInvoiceWithoutPayment() {
        EnrollmentService service = new EnrollmentService(
                enrollmentRepository,
                statusHistoryRepository,
                studentRepository,
                classroomRepository,
                tuitionPackageRepository,
                classPackageRepository,
                studentPackageRepository,
                invoiceRepository,
                classSessionRepository,
                attendanceRepository,
                paymentRepository,
                new EnrollmentMapper(new StudentPackageMapper(), new InvoiceMapper()),
                new StudentMapper()
        );

        Student student = new Student();
        student.setId(1L);
        student.setStatus(StudentStatus.ACTIVE);

        Classroom classroom = new Classroom();
        classroom.setId(2L);
        classroom.setStatus(ClassroomStatus.ONGOING);
        classroom.setStartDate(LocalDate.of(2026, 1, 5));
        classroom.setDaysOfWeek(Set.of(ClassDayOfWeek.MONDAY));

        TuitionPackage tuitionPackage = new TuitionPackage();
        tuitionPackage.setId(3L);
        tuitionPackage.setName("8 buổi");
        tuitionPackage.setTotalSessions(8);
        tuitionPackage.setPrice(new BigDecimal("1000000"));
        tuitionPackage.setStatus(TuitionPackageStatus.ACTIVE);

        when(enrollmentRepository.findByStudentIdAndClassroomIdOrderByStartDateAscIdAsc(1L, 2L))
                .thenReturn(List.of());
        when(classPackageRepository.existsByClassroomIdAndTuitionPackageIdAndActiveTrue(2L, 3L))
                .thenReturn(true);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            enrollment.setId(10L);
            return enrollment;
        });
        when(statusHistoryRepository.save(any(EnrollmentStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studentPackageRepository.save(any(StudentPackage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            invoice.setId(20L);
            return invoice;
        });

        Enrollment created = service.enrollFromLegacyImport(
                student,
                classroom,
                tuitionPackage,
                LocalDate.of(2026, 1, 5)
        );

        assertThat(created.getCreationSource()).isEqualTo(EnrollmentCreationSource.LEGACY_IMPORT);
        assertThat(created.getTotalSessions()).isEqualTo(8);
        assertThat(created.getUsedSessions()).isEqualTo(0);

        ArgumentCaptor<StudentPackage> packageCaptor = ArgumentCaptor.forClass(StudentPackage.class);
        verify(studentPackageRepository).save(packageCaptor.capture());
        assertThat(packageCaptor.getValue().getSourceType()).isEqualTo(StudentPackageSourceType.LEGACY_IMPORT);

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        assertThat(invoiceCaptor.getValue().getPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(paymentRepository, never()).save(any());
    }
}
