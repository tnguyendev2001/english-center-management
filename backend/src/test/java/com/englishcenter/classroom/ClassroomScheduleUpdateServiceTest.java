package com.englishcenter.classroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassroomScheduleUpdateServiceTest {
    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Test
    void applyScheduleChangeCancelsFutureScheduledSessionsWithoutAttendance() {
        ClassroomScheduleUpdateService service = newService();
        ClassSession futureSession = session(10L, LocalDate.now().plusDays(3), ClassSessionStatus.SCHEDULED);

        when(classSessionRepository.countByClassroomId(1L)).thenReturn(1);
        when(classSessionRepository.findByClassroomIdOrderBySessionDateAscStartTimeAsc(1L))
                .thenReturn(List.of(futureSession));
        when(attendanceRepository.existsBySessionId(10L)).thenReturn(false);

        service.applyScheduleChangeIfNeeded(
                1L,
                LocalDate.of(2026, 7, 1),
                Set.of(ClassDayOfWeek.MONDAY, ClassDayOfWeek.WEDNESDAY),
                LocalTime.of(18, 0),
                LocalTime.of(19, 30),
                LocalDate.of(2026, 7, 1),
                Set.of(ClassDayOfWeek.TUESDAY, ClassDayOfWeek.THURSDAY),
                LocalTime.of(18, 30),
                LocalTime.of(20, 0)
        );

        ArgumentCaptor<ClassSession> captor = ArgumentCaptor.forClass(ClassSession.class);
        verify(classSessionRepository).save(captor.capture());
        ClassSession saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(ClassSessionStatus.CANCELED);
        assertThat(saved.getCancelReason()).isEqualTo(ClassroomScheduleUpdateService.SCHEDULE_CHANGE_CANCEL_REASON);
    }

    @Test
    void applyScheduleChangeDoesNotModifyCompletedSessions() {
        ClassroomScheduleUpdateService service = newService();
        ClassSession completedSession = session(11L, LocalDate.now().minusDays(1), ClassSessionStatus.COMPLETED);

        when(classSessionRepository.countByClassroomId(1L)).thenReturn(1);
        when(classSessionRepository.findByClassroomIdOrderBySessionDateAscStartTimeAsc(1L))
                .thenReturn(List.of(completedSession));

        service.applyScheduleChangeIfNeeded(
                1L,
                LocalDate.of(2026, 7, 1),
                Set.of(ClassDayOfWeek.MONDAY),
                LocalTime.of(18, 0),
                LocalTime.of(19, 30),
                LocalDate.of(2026, 7, 2),
                Set.of(ClassDayOfWeek.THURSDAY),
                LocalTime.of(18, 30),
                LocalTime.of(20, 0)
        );

        verify(classSessionRepository, never()).save(any(ClassSession.class));
    }

    @Test
    void applyScheduleChangeRejectsFutureScheduledSessionWithAttendance() {
        ClassroomScheduleUpdateService service = newService();
        ClassSession futureSession = session(12L, LocalDate.now().plusDays(2), ClassSessionStatus.SCHEDULED);

        when(classSessionRepository.countByClassroomId(1L)).thenReturn(1);
        when(classSessionRepository.findByClassroomIdOrderBySessionDateAscStartTimeAsc(1L))
                .thenReturn(List.of(futureSession));
        when(attendanceRepository.existsBySessionId(12L)).thenReturn(true);

        assertThatThrownBy(() -> service.applyScheduleChangeIfNeeded(
                1L,
                LocalDate.of(2026, 7, 1),
                Set.of(ClassDayOfWeek.MONDAY),
                LocalTime.of(18, 0),
                LocalTime.of(19, 30),
                LocalDate.of(2026, 7, 2),
                Set.of(ClassDayOfWeek.THURSDAY),
                LocalTime.of(18, 30),
                LocalTime.of(20, 0)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không thể thay đổi lịch học quá khứ hoặc buổi đã điểm danh.");

        verify(classSessionRepository, never()).save(any(ClassSession.class));
    }

    private ClassroomScheduleUpdateService newService() {
        return new ClassroomScheduleUpdateService(classSessionRepository, attendanceRepository);
    }

    private ClassSession session(Long id, LocalDate sessionDate, ClassSessionStatus status) {
        ClassSession session = new ClassSession();
        session.setId(id);
        session.setSessionDate(sessionDate);
        session.setStatus(status);
        return session;
    }
}
