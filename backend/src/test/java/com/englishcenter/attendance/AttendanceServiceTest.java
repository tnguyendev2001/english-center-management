package com.englishcenter.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishcenter.attendance.dto.AttendanceItemRequest;
import com.englishcenter.attendance.dto.AttendanceResponse;
import com.englishcenter.attendance.dto.MarkAttendanceRequest;
import com.englishcenter.attendance.mapper.AttendanceMapper;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.makeupcredit.MakeupCredit;
import com.englishcenter.makeupcredit.MakeupCreditReason;
import com.englishcenter.makeupcredit.MakeupCreditRepository;
import com.englishcenter.student.Student;
import com.englishcenter.student.StudentRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {
    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private MakeupCreditRepository makeupCreditRepository;

    private final AttendanceMapper attendanceMapper = new AttendanceMapper();

    @Test
    void markRejectsCanceledSession() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.CANCELED);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.mark(markRequest(AttendanceStatus.PRESENT)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot mark attendance for canceled session");

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    void markCreatesMakeupCreditForExcusedAttendance() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.SCHEDULED);
        Student student = student();
        Enrollment enrollment = enrollment(student, session.getClassroom());

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByClassroomIdAndStatus(2L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(enrollment));
        when(studentRepository.findAllById(any())).thenReturn(List.of(student));
        when(attendanceRepository.findBySessionIdAndStudentId(1L, 3L)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance attendance = invocation.getArgument(0);
            attendance.setId(10L);
            return attendance;
        });
        when(makeupCreditRepository.findByStudentIdAndSourceSessionIdAndReason(
                3L,
                1L,
                MakeupCreditReason.EXCUSED_ABSENCE
        )).thenReturn(Optional.empty());
        when(makeupCreditRepository.save(any(MakeupCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<AttendanceResponse> responses = service.mark(markRequest(AttendanceStatus.EXCUSED));

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().status()).isEqualTo(AttendanceStatus.EXCUSED);
        verify(makeupCreditRepository).save(any(MakeupCredit.class));
    }

    @Test
    void markUpdatesExistingAttendanceRecord() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.SCHEDULED);
        Student student = student();
        Enrollment enrollment = enrollment(student, session.getClassroom());
        Attendance existing = new Attendance();
        existing.setId(10L);
        existing.setSession(session);
        existing.setStudent(student);
        existing.setStatus(AttendanceStatus.ABSENT);

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByClassroomIdAndStatus(2L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(enrollment));
        when(studentRepository.findAllById(any())).thenReturn(List.of(student));
        when(attendanceRepository.findBySessionIdAndStudentId(1L, 3L)).thenReturn(Optional.of(existing));
        when(attendanceRepository.save(existing)).thenReturn(existing);

        List<AttendanceResponse> responses = service.mark(markRequest(AttendanceStatus.PRESENT));

        assertThat(responses).hasSize(1);
        assertThat(existing.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        verify(attendanceRepository).save(existing);
    }

    private AttendanceService newService() {
        return new AttendanceService(
                attendanceRepository,
                classSessionRepository,
                enrollmentRepository,
                studentRepository,
                makeupCreditRepository,
                attendanceMapper
        );
    }

    private MarkAttendanceRequest markRequest(AttendanceStatus status) {
        return new MarkAttendanceRequest(
                1L,
                List.of(new AttendanceItemRequest(3L, status, null))
        );
    }

    private ClassSession session(ClassSessionStatus status) {
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
        session.setStatus(status);
        return session;
    }

    private Student student() {
        Student student = new Student();
        student.setId(3L);
        student.setFullName("Nguyen Van A");
        return student;
    }

    private Enrollment enrollment(Student student, Classroom classroom) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(4L);
        enrollment.setStudent(student);
        enrollment.setClassroom(classroom);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        return enrollment;
    }
}
