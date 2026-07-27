package com.englishcenter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.AccountStatus;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.payment.Payment;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.student.Student;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceBillingAccessTest {
    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private ClassSessionRepository classSessionRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private PaymentRepository paymentRepository;

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService(
                classroomRepository,
                classSessionRepository,
                enrollmentRepository,
                invoiceRepository,
                paymentRepository
        );
    }

    @Test
    void teacherCanAccessAssignedClassroomInvoiceOnly() {
        Invoice invoice = invoiceForTeacher(11L);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        assertThat(authorizationService.canAccessInvoice(auth(teacher(11L)), 1L)).isTrue();
        assertThat(authorizationService.canAccessInvoice(auth(teacher(99L)), 1L)).isFalse();
    }

    @Test
    void studentCanAccessOwnInvoiceOnly() {
        Invoice invoice = invoiceForStudent(7L);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        assertThat(authorizationService.canAccessInvoice(auth(student(7L)), 1L)).isTrue();
        assertThat(authorizationService.canAccessInvoice(auth(student(8L)), 1L)).isFalse();
    }

    @Test
    void teacherCanAccessAssignedClassroomPaymentOnly() {
        Payment payment = paymentForTeacher(11L);
        when(paymentRepository.findById(5L)).thenReturn(Optional.of(payment));

        assertThat(authorizationService.canAccessPayment(auth(teacher(11L)), 5L)).isTrue();
        assertThat(authorizationService.canAccessPayment(auth(teacher(99L)), 5L)).isFalse();
    }

    private UsernamePasswordAuthenticationToken auth(AccountPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of());
    }

    private AccountPrincipal teacher(Long teacherId) {
        return new AccountPrincipal(
                1L,
                "teacher",
                AccountRole.TEACHER,
                AccountStatus.ACTIVE,
                null,
                teacherId,
                false,
                0
        );
    }

    private AccountPrincipal student(Long studentId) {
        return new AccountPrincipal(
                2L,
                "student",
                AccountRole.STUDENT,
                AccountStatus.ACTIVE,
                studentId,
                null,
                false,
                0
        );
    }

    private Invoice invoiceForTeacher(Long teacherId) {
        Classroom classroom = new Classroom();
        classroom.setId(3L);
        classroom.setTeacherId(teacherId);
        Student student = new Student();
        student.setId(7L);
        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setClassroom(classroom);
        invoice.setStudent(student);
        return invoice;
    }

    private Invoice invoiceForStudent(Long studentId) {
        Classroom classroom = new Classroom();
        classroom.setId(3L);
        classroom.setTeacherId(11L);
        Student student = new Student();
        student.setId(studentId);
        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setClassroom(classroom);
        invoice.setStudent(student);
        return invoice;
    }

    private Payment paymentForTeacher(Long teacherId) {
        Classroom classroom = new Classroom();
        classroom.setId(3L);
        classroom.setTeacherId(teacherId);
        Student student = new Student();
        student.setId(7L);
        Payment payment = new Payment();
        payment.setId(5L);
        payment.setClassroom(classroom);
        payment.setStudent(student);
        return payment;
    }
}
