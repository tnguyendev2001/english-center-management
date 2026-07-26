package com.englishcenter.enrollment;

import com.englishcenter.attendance.Attendance;
import com.englishcenter.attendance.AttendanceStatus;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.common.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class EnrollmentSessionService {
    private final EnrollmentStatusHistoryRepository statusHistoryRepository;

    public EnrollmentSessionService(EnrollmentStatusHistoryRepository statusHistoryRepository) {
        this.statusHistoryRepository = statusHistoryRepository;
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

        if (statusHistoryRepository.isActiveAt(enrollment.getId(), session.getSessionDate())) {
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
        boolean newConsumes = consumesStatus(newStatus);

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
}
