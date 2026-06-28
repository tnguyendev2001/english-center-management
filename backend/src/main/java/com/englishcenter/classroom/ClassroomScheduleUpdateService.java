package com.englishcenter.classroom;

import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ClassroomScheduleUpdateService {
    static final String SCHEDULE_CHANGE_CANCEL_REASON = "Lịch học thay đổi";

    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRepository attendanceRepository;

    public ClassroomScheduleUpdateService(
            ClassSessionRepository classSessionRepository,
            AttendanceRepository attendanceRepository
    ) {
        this.classSessionRepository = classSessionRepository;
        this.attendanceRepository = attendanceRepository;
    }

    public void applyScheduleChangeIfNeeded(
            Long classroomId,
            LocalDate oldStartDate,
            Set<ClassDayOfWeek> oldDaysOfWeek,
            LocalTime oldStartTime,
            LocalTime oldEndTime,
            LocalDate newStartDate,
            Set<ClassDayOfWeek> newDaysOfWeek,
            LocalTime newStartTime,
            LocalTime newEndTime
    ) {
        if (!isScheduleChanged(
                oldStartDate,
                oldDaysOfWeek,
                oldStartTime,
                oldEndTime,
                newStartDate,
                newDaysOfWeek,
                newStartTime,
                newEndTime
        )) {
            return;
        }

        if (classSessionRepository.countByClassroomId(classroomId) == 0) {
            return;
        }

        validateFutureScheduledSessionsWithAttendance(classroomId);
        cancelFutureScheduledSessionsWithoutAttendance(classroomId);
    }

    static boolean isScheduleChanged(
            LocalDate oldStartDate,
            Set<ClassDayOfWeek> oldDaysOfWeek,
            LocalTime oldStartTime,
            LocalTime oldEndTime,
            LocalDate newStartDate,
            Set<ClassDayOfWeek> newDaysOfWeek,
            LocalTime newStartTime,
            LocalTime newEndTime
    ) {
        return !Objects.equals(oldStartDate, newStartDate)
                || !Objects.equals(oldDaysOfWeek, newDaysOfWeek)
                || !Objects.equals(oldStartTime, newStartTime)
                || !Objects.equals(oldEndTime, newEndTime);
    }

    private void validateFutureScheduledSessionsWithAttendance(Long classroomId) {
        LocalDate today = LocalDate.now();
        List<ClassSession> sessions = classSessionRepository.findByClassroomIdOrderBySessionDateAscStartTimeAsc(
                classroomId
        );

        for (ClassSession session : sessions) {
            if (session.getStatus() != ClassSessionStatus.SCHEDULED) {
                continue;
            }

            if (session.getSessionDate().isBefore(today)) {
                continue;
            }

            if (attendanceRepository.existsBySessionId(session.getId())) {
                throw new BusinessException(
                        "Không thể thay đổi lịch học quá khứ hoặc buổi đã điểm danh."
                );
            }
        }
    }

    private void cancelFutureScheduledSessionsWithoutAttendance(Long classroomId) {
        LocalDate today = LocalDate.now();
        List<ClassSession> sessions = classSessionRepository.findByClassroomIdOrderBySessionDateAscStartTimeAsc(
                classroomId
        );

        for (ClassSession session : sessions) {
            if (session.getStatus() != ClassSessionStatus.SCHEDULED) {
                continue;
            }

            if (session.getSessionDate().isBefore(today)) {
                continue;
            }

            if (attendanceRepository.existsBySessionId(session.getId())) {
                continue;
            }

            session.setStatus(ClassSessionStatus.CANCELED);
            session.setCancelReason(SCHEDULE_CHANGE_CANCEL_REASON);
            classSessionRepository.save(session);
        }
    }
}
