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
import com.englishcenter.classroom.ClassroomStatus;
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
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import com.englishcenter.student.Student;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.enrollment.EnrollmentLearningDateHelper;
import com.englishcenter.enrollment.EnrollmentSessionService;
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
    private final EnrollmentSessionService enrollmentSessionService = new EnrollmentSessionService();

    @Test
    void markRejectsNonOngoingClassroom() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.SCHEDULED);
        session.getClassroom().setStatus(ClassroomStatus.PLANNED);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.mark(markRequest(AttendanceStatus.PRESENT)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot mark attendance for classroom that is not ongoing");

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

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
        mockEligibleEnrollments(session, enrollment);
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
        existing.setValid(true);

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        mockEligibleEnrollments(session, enrollment, 12, 0);
        when(attendanceRepository.findBySessionIdAndStudentId(1L, 3L)).thenReturn(Optional.of(existing));
        when(attendanceRepository.save(existing)).thenReturn(existing);

        List<AttendanceResponse> responses = service.mark(markRequest(AttendanceStatus.PRESENT));

        assertThat(responses).hasSize(1);
        assertThat(existing.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        verify(attendanceRepository).save(existing);
    }

    @Test
    void markExcusedToPresentCancelsMakeupCredit() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.COMPLETED);
        Student student = student();
        Enrollment enrollment = enrollment(student, session.getClassroom());
        Attendance existing = new Attendance();
        existing.setId(10L);
        existing.setSession(session);
        existing.setStudent(student);
        existing.setStatus(AttendanceStatus.EXCUSED);
        existing.setValid(true);
        MakeupCredit credit = new MakeupCredit();
        credit.setStatus(MakeupCreditStatus.AVAILABLE);
        credit.setUsedSessions(0);

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        mockEligibleEnrollments(session, enrollment, 12, 0);
        when(attendanceRepository.findBySessionIdAndStudentId(1L, 3L)).thenReturn(Optional.of(existing));
        when(attendanceRepository.save(existing)).thenReturn(existing);
        when(makeupCreditRepository.findByStudentIdAndSourceSessionIdAndReason(
                3L,
                1L,
                MakeupCreditReason.EXCUSED_ABSENCE
        )).thenReturn(Optional.of(credit));
        when(makeupCreditRepository.save(credit)).thenReturn(credit);

        service.mark(markRequest(AttendanceStatus.PRESENT, "Marked present by mistake"));

        assertThat(existing.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(credit.getStatus()).isEqualTo(MakeupCreditStatus.CANCELED);
    }

    @Test
    void markExcusedToPresentCancelsLegacyUsedMakeupCredit() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.COMPLETED);
        Student student = student();
        Enrollment enrollment = enrollment(student, session.getClassroom());
        Attendance existing = new Attendance();
        existing.setSession(session);
        existing.setStudent(student);
        existing.setStatus(AttendanceStatus.EXCUSED);
        existing.setValid(true);
        MakeupCredit credit = new MakeupCredit();
        credit.setStatus(MakeupCreditStatus.USED);
        credit.setUsedSessions(1);

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        mockEligibleEnrollments(session, enrollment, 12, 0);
        when(attendanceRepository.findBySessionIdAndStudentId(1L, 3L)).thenReturn(Optional.of(existing));
        when(makeupCreditRepository.findByStudentIdAndSourceSessionIdAndReason(
                3L,
                1L,
                MakeupCreditReason.EXCUSED_ABSENCE
        )).thenReturn(Optional.of(credit));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.mark(markRequest(AttendanceStatus.PRESENT, "Marked present by mistake"));

        assertThat(existing.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(credit.getStatus()).isEqualTo(MakeupCreditStatus.CANCELED);
    }

    @Test
    void markExcusedToPresentRequiresCorrectionReason() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.COMPLETED);
        Student student = student();
        Enrollment enrollment = enrollment(student, session.getClassroom());
        Attendance existing = new Attendance();
        existing.setSession(session);
        existing.setStudent(student);
        existing.setStatus(AttendanceStatus.EXCUSED);

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        mockEligibleEnrollments(session, enrollment, 12, 0);
        when(attendanceRepository.findBySessionIdAndStudentId(1L, 3L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.mark(markRequest(AttendanceStatus.PRESENT, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Correction reason is required when changing excused attendance");
    }

    @Test
    void markPresentToExcusedReactivatesCanceledMakeupCredit() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.COMPLETED);
        Student student = student();
        Enrollment enrollment = enrollment(student, session.getClassroom());
        Attendance existing = new Attendance();
        existing.setId(10L);
        existing.setSession(session);
        existing.setStudent(student);
        existing.setStatus(AttendanceStatus.PRESENT);
        existing.setValid(true);
        MakeupCredit credit = new MakeupCredit();
        credit.setStatus(MakeupCreditStatus.CANCELED);
        credit.setUsedSessions(0);

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        mockEligibleEnrollments(session, enrollment, 12, 0);
        when(attendanceRepository.findBySessionIdAndStudentId(1L, 3L)).thenReturn(Optional.of(existing));
        when(attendanceRepository.save(existing)).thenReturn(existing);
        when(makeupCreditRepository.findByStudentIdAndSourceSessionIdAndReason(
                3L,
                1L,
                MakeupCreditReason.EXCUSED_ABSENCE
        )).thenReturn(Optional.of(credit));
        when(makeupCreditRepository.save(credit)).thenReturn(credit);

        service.mark(markRequest(AttendanceStatus.EXCUSED, null));

        assertThat(existing.getStatus()).isEqualTo(AttendanceStatus.EXCUSED);
        assertThat(credit.getStatus()).isEqualTo(MakeupCreditStatus.AVAILABLE);
    }

    @Test
    void markReactivatesVoidedPresentAttendanceAndConsumesSession() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.SCHEDULED);
        Student student = student();
        Enrollment enrollment = enrollment(student, session.getClassroom());
        Attendance existing = new Attendance();
        existing.setId(10L);
        existing.setSession(session);
        existing.setStudent(student);
        existing.setStatus(AttendanceStatus.PRESENT);
        existing.setValid(false);
        existing.setVoidReason("Hoàn tác điểm danh");

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        mockEligibleEnrollments(session, enrollment, 4, 0);
        when(attendanceRepository.findBySessionIdAndStudentId(1L, 3L)).thenReturn(Optional.of(existing));
        when(attendanceRepository.save(existing)).thenReturn(existing);
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        service.mark(markRequest(AttendanceStatus.PRESENT));

        assertThat(enrollment.getUsedSessions()).isEqualTo(1);
        assertThat(existing.getValid()).isTrue();
        assertThat(existing.getVoidReason()).isNull();
        assertThat(existing.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    void markPresentToExcusedDecrementsUsedSessions() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.COMPLETED);
        Student student = student();
        Enrollment enrollment = enrollment(student, session.getClassroom());
        Attendance existing = new Attendance();
        existing.setId(10L);
        existing.setSession(session);
        existing.setStudent(student);
        existing.setStatus(AttendanceStatus.PRESENT);
        existing.setValid(true);

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        mockEligibleEnrollments(session, enrollment, 4, 1);
        when(attendanceRepository.findBySessionIdAndStudentId(1L, 3L)).thenReturn(Optional.of(existing));
        when(attendanceRepository.save(existing)).thenReturn(existing);
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);
        when(makeupCreditRepository.findByStudentIdAndSourceSessionIdAndReason(
                3L,
                1L,
                MakeupCreditReason.EXCUSED_ABSENCE
        )).thenReturn(Optional.empty());
        when(makeupCreditRepository.save(any(MakeupCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.mark(markRequest(AttendanceStatus.EXCUSED));

        assertThat(enrollment.getUsedSessions()).isZero();
    }

    @Test
    void markExcusedToAbsentIncrementsUsedSessions() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.COMPLETED);
        Student student = student();
        Enrollment enrollment = enrollment(student, session.getClassroom());
        Attendance existing = new Attendance();
        existing.setId(10L);
        existing.setSession(session);
        existing.setStudent(student);
        existing.setStatus(AttendanceStatus.EXCUSED);
        existing.setValid(true);

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        mockEligibleEnrollments(session, enrollment, 4, 0);
        when(attendanceRepository.findBySessionIdAndStudentId(1L, 3L)).thenReturn(Optional.of(existing));
        when(attendanceRepository.save(existing)).thenReturn(existing);
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);
        when(makeupCreditRepository.findByStudentIdAndSourceSessionIdAndReason(
                3L,
                1L,
                MakeupCreditReason.EXCUSED_ABSENCE
        )).thenReturn(Optional.empty());

        service.mark(markRequest(AttendanceStatus.ABSENT, "Corrected from excused"));

        assertThat(enrollment.getUsedSessions()).isEqualTo(1);
    }

    @Test
    void markRejectsStudentNotYetStarted() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.SCHEDULED);
        Student student = student();
        Enrollment enrollment = enrollment(student, session.getClassroom());
        enrollment.setStartDate(LocalDate.of(2026, 7, 8));

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findEligibleForAttendanceBySessionDate(
                2L,
                EnrollmentStatus.ACTIVE,
                session.getSessionDate()
        )).thenReturn(List.of());
        when(enrollmentRepository.findByClassroomIdAndStatus(2L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(enrollment));

        assertThatThrownBy(() -> service.mark(markRequest(AttendanceStatus.PRESENT)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(EnrollmentLearningDateHelper.NOT_STARTED_FOR_SESSION_MESSAGE);

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    void markRejectsStudentAfterLearningEndDate() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.SCHEDULED);
        session.setSessionDate(LocalDate.of(2026, 7, 21));
        Student student = student();
        Enrollment enrollment = enrollment(student, session.getClassroom());
        enrollment.setStartDate(LocalDate.of(2026, 7, 1));
        enrollment.setEndDate(LocalDate.of(2026, 7, 20));

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findEligibleForAttendanceBySessionDate(
                2L,
                EnrollmentStatus.ACTIVE,
                session.getSessionDate()
        )).thenReturn(List.of());
        when(enrollmentRepository.findByClassroomIdAndStatus(2L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(enrollment));

        assertThatThrownBy(() -> service.mark(markRequest(AttendanceStatus.PRESENT)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(EnrollmentLearningDateHelper.ENDED_FOR_SESSION_MESSAGE);

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    private AttendanceService newService() {
        return new AttendanceService(
                attendanceRepository,
                classSessionRepository,
                enrollmentRepository,
                studentRepository,
                makeupCreditRepository,
                enrollmentSessionService,
                attendanceMapper
        );
    }

    private void mockEligibleEnrollments(ClassSession session, Enrollment enrollment) {
        mockEligibleEnrollments(session, enrollment, 12, 0);
    }

    private void mockEligibleEnrollments(
            ClassSession session,
            Enrollment enrollment,
            int totalSessions,
            int usedSessions
    ) {
        mockEnrollmentSessions(enrollment, totalSessions, usedSessions);
        enrollment.setStartDate(session.getSessionDate());
        when(enrollmentRepository.findEligibleForAttendanceBySessionDate(
                session.getClassroom().getId(),
                EnrollmentStatus.ACTIVE,
                session.getSessionDate()
        )).thenReturn(List.of(enrollment));
    }

    private void mockEnrollmentSessions(Enrollment enrollment, int totalSessions, int usedSessions) {
        enrollment.setTotalSessions(totalSessions);
        enrollment.setUsedSessions(usedSessions);
    }

    private MarkAttendanceRequest markRequest(AttendanceStatus status) {
        return markRequest(status, null);
    }

    private MarkAttendanceRequest markRequest(AttendanceStatus status, String correctionReason) {
        return new MarkAttendanceRequest(
                1L,
                List.of(new AttendanceItemRequest(3L, status, null, correctionReason))
        );
    }

    private ClassSession session(ClassSessionStatus status) {
        Classroom classroom = new Classroom();
        classroom.setId(2L);
        classroom.setClassName("Starter A");
        classroom.setStatus(ClassroomStatus.ONGOING);

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
        enrollment.setStartDate(LocalDate.of(2026, 7, 1));
        return enrollment;
    }
}
