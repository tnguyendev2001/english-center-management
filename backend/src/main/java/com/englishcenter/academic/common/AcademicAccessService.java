package com.englishcenter.academic.common;

import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AcademicAccessService {
    private final ClassroomRepository classroomRepository;
    private final ClassSessionRepository classSessionRepository;
    private final EnrollmentRepository enrollmentRepository;

    public AcademicAccessService(
            ClassroomRepository classroomRepository,
            ClassSessionRepository classSessionRepository,
            EnrollmentRepository enrollmentRepository
    ) {
        this.classroomRepository = classroomRepository;
        this.classSessionRepository = classSessionRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public void requireManageClassroom(Long classroomId) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.ADMIN) {
            return;
        }
        if (principal.role() == AccountRole.TEACHER
                && principal.teacherId() != null
                && classroomRepository.existsByIdAndTeacherId(classroomId, principal.teacherId())) {
            return;
        }
        throw new AccessDeniedException("Bạn không có quyền quản lý lớp học này.");
    }

    public void requireAccessClassroom(Long classroomId) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.ADMIN) {
            return;
        }
        if (principal.role() == AccountRole.TEACHER
                && principal.teacherId() != null
                && classroomRepository.existsByIdAndTeacherId(classroomId, principal.teacherId())) {
            return;
        }
        if (principal.role() == AccountRole.STUDENT
                && principal.studentId() != null
                && enrollmentRepository.existsByStudentIdAndClassroomId(principal.studentId(), classroomId)) {
            return;
        }
        throw new AccessDeniedException("Bạn không có quyền truy cập lớp học này.");
    }

    public void requireManageSession(Long sessionId) {
        requireManageClassroom(classroomIdOfSession(sessionId));
    }

    public Long classroomIdOfSession(Long sessionId) {
        return classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy buổi học."))
                .getClassroom()
                .getId();
    }

    public boolean isEnrolled(Long studentId, Long classroomId) {
        return enrollmentRepository.existsByStudentIdAndClassroomId(studentId, classroomId);
    }

    public boolean isActiveEnrolled(Long studentId, Long classroomId) {
        return enrollmentRepository.existsByStudentIdAndClassroomIdAndStatus(
                studentId, classroomId, EnrollmentStatus.ACTIVE);
    }

    public void requireSelfOrManagerOfStudent(Long studentId) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.ADMIN) {
            return;
        }
        if (principal.role() == AccountRole.STUDENT
                && principal.studentId() != null
                && principal.studentId().equals(studentId)) {
            return;
        }
        if (principal.role() == AccountRole.TEACHER
                && principal.teacherId() != null
                && enrollmentRepository.existsByStudentIdAndClassroomTeacherId(studentId, principal.teacherId())) {
            return;
        }
        throw new AccessDeniedException("Bạn không có quyền truy cập dữ liệu học viên này.");
    }
}
