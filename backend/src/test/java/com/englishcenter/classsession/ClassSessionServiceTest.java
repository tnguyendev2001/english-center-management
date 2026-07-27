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
import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.AccountStatus;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classroom.ClassroomStatus;
import com.englishcenter.classsession.dto.CancelClassSessionRequest;
import com.englishcenter.classsession.dto.ClassSessionResponse;
import com.englishcenter.classsession.dto.CreateClassSessionRequest;
import com.englishcenter.classsession.mapper.ClassSessionMapper;
import com.englishcenter.common.config.AppTimeProperties;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentSessionService;
import com.englishcenter.enrollment.EnrollmentStatusHistoryRepository;
import com.englishcenter.makeupcredit.MakeupCredit;
import com.englishcenter.makeupcredit.MakeupCreditRepository;
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import com.englishcenter.student.Student;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private EnrollmentStatusHistoryRepository statusHistoryRepository;

    private final ClassSessionMapper classSessionMapper = new ClassSessionMapper();
    private final AppTimeProperties appTimeProperties = new AppTimeProperties();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void teacherCanCreateSessionForAssignedClassroom() {
        ClassSessionService service = newService();
        Classroom classroom = activeClassroom(10L);
        authenticateTeacher(10L);
        LocalDate nextTuesday = nextWeekday(ClassDayOfWeek.TUESDAY);
        CreateClassSessionRequest request = new CreateClassSessionRequest(
                2L,
                nextTuesday,
                LocalTime.of(18, 0),
                LocalTime.of(19, 30),
                "Buổi học thường"
        );

        when(classroomRepository.findById(2L)).thenReturn(Optional.of(classroom));
        when(classSessionRepository.existsByClassroomIdAndSessionDateAndStartTimeAndEndTime(
                2L, request.sessionDate(), request.startTime(), request.endTime()
        )).thenReturn(false);
        when(classSessionRepository.countByClassroomId(2L)).thenReturn(3);
        when(classSessionRepository.save(any(ClassSession.class))).thenAnswer(invocation -> {
            ClassSession session = invocation.getArgument(0);
            session.setId(99L);
            return session;
        });

        ClassSessionResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.status()).isEqualTo(ClassSessionStatus.SCHEDULED);
        assertThat(response.classroomId()).isEqualTo(2L);
        assertThat(response.sessionNo()).isEqualTo(4);
    }

    @Test
    void teacherCannotCreateSessionForOtherClassroom() {
        ClassSessionService service = newService();
        Classroom classroom = activeClassroom(11L);
        authenticateTeacher(10L);
        CreateClassSessionRequest request = new CreateClassSessionRequest(
                2L,
                LocalDate.of(2026, 7, 28),
                LocalTime.of(18, 0),
                LocalTime.of(19, 30),
                null
        );

        when(classroomRepository.findById(2L)).thenReturn(Optional.of(classroom));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Bạn không có quyền tạo buổi học cho lớp này.");
        verify(classSessionRepository, never()).save(any());
    }

    @Test
    void teacherCannotCreatePastSession() {
        ClassSessionService service = newService();
        Classroom classroom = activeClassroom(10L);
        authenticateTeacher(10L);
        CreateClassSessionRequest request = new CreateClassSessionRequest(
                2L,
                LocalDate.of(2020, 1, 7),
                LocalTime.of(18, 0),
                LocalTime.of(19, 30),
                null
        );

        when(classroomRepository.findById(2L)).thenReturn(Optional.of(classroom));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không thể tạo buổi học trong quá khứ.");
        verify(classSessionRepository, never()).save(any());
    }

    @Test
    void createRejectsOffScheduleDate() {
        ClassSessionService service = newService();
        Classroom classroom = activeClassroom(10L);
        authenticateTeacher(10L);
        // 2026-07-29 is Wednesday; classroom is Tue/Fri
        CreateClassSessionRequest request = new CreateClassSessionRequest(
                2L,
                LocalDate.of(2026, 7, 29),
                LocalTime.of(18, 0),
                LocalTime.of(19, 30),
                null
        );

        when(classroomRepository.findById(2L)).thenReturn(Optional.of(classroom));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ngày học không khớp với lịch học của lớp");
        verify(classSessionRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateSession() {
        ClassSessionService service = newService();
        Classroom classroom = activeClassroom(10L);
        authenticateTeacher(10L);
        LocalDate nextTuesday = nextWeekday(ClassDayOfWeek.TUESDAY);
        CreateClassSessionRequest request = new CreateClassSessionRequest(
                2L,
                nextTuesday,
                LocalTime.of(18, 0),
                LocalTime.of(19, 30),
                null
        );

        when(classroomRepository.findById(2L)).thenReturn(Optional.of(classroom));
        when(classSessionRepository.existsByClassroomIdAndSessionDateAndStartTimeAndEndTime(
                2L, request.sessionDate(), request.startTime(), request.endTime()
        )).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lớp đã có buổi học vào thời gian này.");
        verify(classSessionRepository, never()).save(any());
    }

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
    void correctionCancelCancelsAvailableMakeupCredit() {
        ClassSessionService service = newService();
        ClassSession session = completedSession();
        MakeupCredit availableCredit = new MakeupCredit();
        availableCredit.setStatus(MakeupCreditStatus.AVAILABLE);

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(attendanceRepository.existsBySessionId(1L)).thenReturn(true);
        when(makeupCreditRepository.findBySourceSessionId(1L)).thenReturn(List.of(availableCredit));
        when(attendanceRepository.findBySessionId(1L)).thenReturn(List.of());
        when(classSessionRepository.save(session)).thenReturn(session);

        service.correctionCancel(1L, new CancelClassSessionRequest("Marked wrong session"));

        assertThat(availableCredit.getStatus()).isEqualTo(MakeupCreditStatus.CANCELED);
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
                enrollmentRepository,
                new EnrollmentSessionService(statusHistoryRepository, enrollmentRepository, attendanceRepository),
                classSessionMapper,
                appTimeProperties
        );
    }

    private void authenticateTeacher(Long teacherId) {
        AccountPrincipal principal = new AccountPrincipal(
                1L,
                "teacher1",
                AccountRole.TEACHER,
                AccountStatus.ACTIVE,
                null,
                teacherId,
                false,
                1
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
                )
        );
    }

    private Classroom activeClassroom(Long teacherId) {
        Classroom classroom = new Classroom();
        classroom.setId(2L);
        classroom.setClassName("Starter A");
        classroom.setTeacherId(teacherId);
        classroom.setStatus(ClassroomStatus.ONGOING);
        classroom.setStartDate(LocalDate.of(2020, 1, 1));
        classroom.setDaysOfWeek(Set.of(ClassDayOfWeek.TUESDAY, ClassDayOfWeek.FRIDAY));
        classroom.setStartTime(LocalTime.of(18, 0));
        classroom.setEndTime(LocalTime.of(19, 30));
        classroom.setRoom("P1");
        return classroom;
    }

    private LocalDate nextWeekday(ClassDayOfWeek dayOfWeek) {
        LocalDate cursor = LocalDate.now(appTimeProperties.zoneId());
        for (int i = 0; i < 14; i++) {
            if (cursor.getDayOfWeek() == dayOfWeek.toJavaDayOfWeek()) {
                return cursor;
            }
            cursor = cursor.plusDays(1);
        }
        return cursor;
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
