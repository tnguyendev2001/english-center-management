package com.englishcenter.classsession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishcenter.attendance.Attendance;
import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.attendance.AttendanceStatus;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classsession.dto.CancelClassSessionRequest;
import com.englishcenter.classsession.mapper.ClassSessionMapper;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.makeupcredit.MakeupCredit;
import com.englishcenter.makeupcredit.MakeupCreditRepository;
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import com.englishcenter.student.Student;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassSessionServiceTest {
    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private MakeupCreditRepository makeupCreditRepository;

    private final ClassSessionMapper classSessionMapper = new ClassSessionMapper();

    @Test
    void correctionCancelVoidsAttendanceAndCancelsSession() {
        ClassSessionService service = newService();
        ClassSession session = completedSession();
        Attendance attendance = attendance(session);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(attendanceRepository.existsBySessionId(1L)).thenReturn(true);
        when(makeupCreditRepository.findBySourceSessionId(1L)).thenReturn(List.of());
        when(attendanceRepository.findBySessionId(1L)).thenReturn(List.of(attendance));
        when(classSessionRepository.save(session)).thenReturn(session);

        service.correctionCancel(1L, new CancelClassSessionRequest("Marked wrong session"));

        assertThat(session.getStatus()).isEqualTo(ClassSessionStatus.CANCELED);
        assertThat(session.getCancelReason()).isEqualTo("Marked wrong session");
        assertThat(attendance.getValid()).isFalse();
        assertThat(attendance.getVoidReason()).isEqualTo("Marked wrong session");
        assertThat(attendance.getVoidedAt()).isNotNull();
        verify(attendanceRepository).save(attendance);
    }

    @Test
    void correctionCancelRejectsUsedMakeupCredit() {
        ClassSessionService service = newService();
        ClassSession session = completedSession();
        MakeupCredit usedCredit = new MakeupCredit();
        usedCredit.setStatus(MakeupCreditStatus.USED);
        usedCredit.setUsedSessions(1);

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(attendanceRepository.existsBySessionId(1L)).thenReturn(true);
        when(makeupCreditRepository.findBySourceSessionId(1L)).thenReturn(List.of(usedCredit));

        assertThatThrownBy(() -> service.correctionCancel(1L, new CancelClassSessionRequest("Too late")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot correction-cancel session: makeup credit from this session has already been used");

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    void cancelRejectsCompletedSession() {
        ClassSessionService service = newService();
        ClassSession session = completedSession();
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.cancel(1L, new CancelClassSessionRequest("Try normal cancel")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot cancel completed session");
    }

    @Test
    void correctionCancelCancelsAvailableMakeupCredits() {
        ClassSessionService service = newService();
        ClassSession session = completedSession();
        MakeupCredit credit = new MakeupCredit();
        credit.setStatus(MakeupCreditStatus.AVAILABLE);
        credit.setUsedSessions(0);

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(attendanceRepository.existsBySessionId(1L)).thenReturn(true);
        when(makeupCreditRepository.findBySourceSessionId(1L)).thenReturn(List.of(credit));
        when(attendanceRepository.findBySessionId(1L)).thenReturn(List.of());
        when(classSessionRepository.save(session)).thenReturn(session);
        when(makeupCreditRepository.save(credit)).thenReturn(credit);

        service.correctionCancel(1L, new CancelClassSessionRequest("Wrong day"));

        ArgumentCaptor<MakeupCredit> captor = ArgumentCaptor.forClass(MakeupCredit.class);
        verify(makeupCreditRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MakeupCreditStatus.CANCELED);
    }

    private ClassSessionService newService() {
        return new ClassSessionService(
                classSessionRepository,
                classroomRepository,
                attendanceRepository,
                makeupCreditRepository,
                classSessionMapper
        );
    }

    private ClassSession completedSession() {
        Classroom classroom = new Classroom();
        classroom.setId(2L);
        classroom.setClassName("Starter A");

        ClassSession session = new ClassSession();
        session.setId(1L);
        session.setClassroom(classroom);
        session.setSessionNo(1);
        session.setSessionDate(LocalDate.of(2026, 7, 1));
        session.setStartTime(LocalTime.of(18, 0));
        session.setEndTime(LocalTime.of(19, 30));
        session.setStatus(ClassSessionStatus.COMPLETED);
        return session;
    }

    private Attendance attendance(ClassSession session) {
        Student student = new Student();
        student.setId(3L);
        student.setFullName("Nguyen Van A");

        Attendance attendance = new Attendance();
        attendance.setId(10L);
        attendance.setSession(session);
        attendance.setStudent(student);
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendance.setValid(true);
        return attendance;
    }
}
