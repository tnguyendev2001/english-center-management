package com.englishcenter.security;

import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.payment.PaymentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("authorizationService")
@Transactional(readOnly = true)
public class AuthorizationService {
    private final ClassroomRepository classroomRepository;
    private final ClassSessionRepository classSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public AuthorizationService(
            ClassroomRepository classroomRepository,
            ClassSessionRepository classSessionRepository,
            EnrollmentRepository enrollmentRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository
    ) {
        this.classroomRepository = classroomRepository;
        this.classSessionRepository = classSessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    public boolean canAccessClassroom(Authentication authentication, Long classroomId) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal(authentication);
        if (principal.role() == AccountRole.ADMIN) {
            return true;
        }
        if (principal.role() == AccountRole.TEACHER) {
            return isAssignedTeacher(principal, classroomId);
        }
        if (principal.role() == AccountRole.STUDENT) {
            return principal.studentId() != null
                    && enrollmentRepository.existsByStudentIdAndClassroomId(
                    principal.studentId(),
                    classroomId
            );
        }
        return false;
    }

    public boolean canManageClassroom(Authentication authentication, Long classroomId) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal(authentication);
        if (principal.role() == AccountRole.ADMIN) {
            return true;
        }
        if (principal.role() == AccountRole.TEACHER) {
            return isAssignedTeacher(principal, classroomId);
        }
        return false;
    }

    public boolean canAccessStudent(Authentication authentication, Long studentId) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal(authentication);
        if (principal.role() == AccountRole.ADMIN) {
            return true;
        }
        if (principal.role() == AccountRole.STUDENT) {
            return principal.studentId() != null && principal.studentId().equals(studentId);
        }
        if (principal.role() == AccountRole.TEACHER) {
            return principal.teacherId() != null
                    && enrollmentRepository.existsByStudentIdAndClassroomTeacherId(
                    studentId,
                    principal.teacherId()
            );
        }
        return false;
    }

    public boolean canManageAttendance(Authentication authentication, Long sessionId) {
        return canManageSession(authentication, sessionId);
    }

    public boolean canAccessSession(Authentication authentication, Long sessionId) {
        return classSessionRepository.findById(sessionId)
                .map(session -> canAccessClassroom(authentication, session.getClassroom().getId()))
                .orElse(false);
    }

    public boolean canManageSession(Authentication authentication, Long sessionId) {
        return classSessionRepository.findById(sessionId)
                .map(session -> canManageClassroom(authentication, session.getClassroom().getId()))
                .orElse(false);
    }

    public boolean canAccessInvoice(Authentication authentication, Long invoiceId) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal(authentication);
        if (principal.role() == AccountRole.ADMIN) {
            return true;
        }
        if (principal.role() != AccountRole.STUDENT || principal.studentId() == null) {
            return false;
        }
        return invoiceRepository.findById(invoiceId)
                .map(invoice -> principal.studentId().equals(invoice.getStudent().getId()))
                .orElse(false);
    }

    public boolean canAccessOwnTeacherProfile(Authentication authentication, Long teacherId) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal(authentication);
        return principal.role() == AccountRole.ADMIN
                || (principal.role() == AccountRole.TEACHER
                && principal.teacherId() != null
                && principal.teacherId().equals(teacherId));
    }

    public boolean canAccessPayment(Authentication authentication, Long paymentId) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal(authentication);
        if (principal.role() == AccountRole.ADMIN) {
            return true;
        }
        if (principal.role() != AccountRole.STUDENT || principal.studentId() == null) {
            return false;
        }
        return paymentRepository.findById(paymentId)
                .map(payment -> principal.studentId().equals(payment.getStudent().getId()))
                .orElse(false);
    }

    private boolean isAssignedTeacher(AccountPrincipal principal, Long classroomId) {
        if (principal.teacherId() == null || classroomId == null) {
            return false;
        }
        return classroomRepository.existsByIdAndTeacherId(classroomId, principal.teacherId());
    }
}
