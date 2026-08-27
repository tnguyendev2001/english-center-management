package com.englishcenter.enrollment;

import com.englishcenter.attendance.Attendance;
import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.attendance.AttendanceStatus;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentSessionService {
    private final EnrollmentEligibilityService enrollmentEligibilityService;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;

    public EnrollmentSessionService(
            EnrollmentEligibilityService enrollmentEligibilityService,
            EnrollmentRepository enrollmentRepository,
            AttendanceRepository attendanceRepository
    ) {
        this.enrollmentEligibilityService = enrollmentEligibilityService;
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceRepository = attendanceRepository;
    }

    public int remainingSessions(Enrollment enrollment) {
        return Math.max(enrollment.getTotalSessions() - enrollment.getUsedSessions(), 0);
    }

    public int overusedSessions(Enrollment enrollment) {
        return Math.max(enrollment.getUsedSessions() - enrollment.getTotalSessions(), 0);
    }

    public boolean consumesSession(Attendance attendance, ClassSession session, Enrollment enrollment) {
        if (attendance == null || session == null || enrollment == null) {
            return false;
        }

        if (!Boolean.TRUE.equals(attendance.getValid())) {
            return false;
        }

        if (session.getStatus() == ClassSessionStatus.CANCELED) {
            return false;
        }

        if (!consumesStatus(attendance.getStatus())) {
            return false;
        }

        if (enrollmentEligibilityService.isActiveOnDate(enrollment, session.getSessionDate())) {
            return true;
        }

        // Historical PRESENT/ABSENT for this same session must remain editable after stop/hold
        // without treating the old mark as non-consuming (which would reverse usedSessions).
        return attendance.getSession() != null
                && attendance.getSession().getId().equals(session.getId());
    }

    public boolean consumesStatus(AttendanceStatus status) {
        return status == AttendanceStatus.PRESENT || status == AttendanceStatus.ABSENT;
    }

    public void applyAttendanceDelta(
            Enrollment enrollment,
            Attendance existingAttendance,
            ClassSession session,
            AttendanceStatus newStatus
    ) {
        boolean oldConsumes = consumesSession(existingAttendance, session, enrollment);
        boolean newConsumes = session.getStatus() != ClassSessionStatus.CANCELED
                && consumesStatus(newStatus);

        if (!oldConsumes && newConsumes) {
            if (remainingSessions(enrollment) <= 0) {
                throw new BusinessException("Học viên đã hết buổi. Vui lòng gia hạn gói trước khi điểm danh.");
            }
            enrollment.setUsedSessions(enrollment.getUsedSessions() + 1);
            return;
        }

        if (oldConsumes && !newConsumes) {
            enrollment.setUsedSessions(Math.max(enrollment.getUsedSessions() - 1, 0));
        }
    }

    public void reverseConsumedSession(Enrollment enrollment) {
        enrollment.setUsedSessions(Math.max(enrollment.getUsedSessions() - 1, 0));
    }

    /**
     * Authoritative progress sync from consuming attendance records.
     * Shared by legacy import and usable for attendance repair.
     */
    @Transactional
    public Enrollment recalculateEnrollmentProgress(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found"));

        List<Attendance> attendances = attendanceRepository.findValidByStudentIdAndClassroomId(
                enrollment.getStudent().getId(),
                enrollment.getClassroom().getId()
        );

        int used = 0;
        for (Attendance attendance : attendances) {
            if (consumesSession(attendance, attendance.getSession(), enrollment)) {
                used++;
            }
        }

        enrollment.setUsedSessions(used);
        return enrollmentRepository.save(enrollment);
    }
}
