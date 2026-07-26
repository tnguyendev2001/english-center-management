package com.englishcenter.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishcenter.attendance.dto.AttendanceItemRequest;
import com.englishcenter.attendance.dto.AttendanceResponse;
import com.englishcenter.attendance.dto.AttendanceReadinessResponse;
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
import com.englishcenter.enrollment.EnrollmentStatusHistoryRepository;
import com.englishcenter.makeupcredit.MakeupCredit;
import com.englishcenter.makeupcredit.MakeupCreditReason;
import com.englishcenter.makeupcredit.MakeupCreditRepository;
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import com.englishcenter.student.Student;
import com.englishcenter.student.StudentRepository;
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

    @Mock
    private EnrollmentStatusHistoryRepository statusHistoryRepository;

    private final AttendanceMapper attendanceMapper = new AttendanceMapper();

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
    void markHistoricalStoppedPresentToExcusedDecrementsUsedSessions() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.COMPLETED);
        Student student = student();
        Enrollment enrollment = enrollment(student, session.getClassroom());
        enrollment.setStatus(EnrollmentStatus.STOPPED);
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
    void markRejectsStudentNotActivelyEnrolledOnSessionDate() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.SCHEDULED);
        Student student = student();
        Enrollment enrollment = enrollment(student, session.getClassroom());
        enrollment.setStartDate(LocalDate.of(2026, 7, 8));

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findEligibleForAttendanceBySessionDate(
                2L,
                session.getSessionDate()
        )).thenReturn(List.of());
        when(attendanceRepository.findBySessionId(1L)).thenReturn(List.of());
        when(attendanceRepository.findBySessionIdAndStudentId(1L, 3L)).thenReturn(Optional.empty());
        when(enrollmentRepository.findByStudentIdAndClassroomIdOrderByStartDateAscIdAsc(3L, 2L))
                .thenReturn(List.of(enrollment));
        when(statusHistoryRepository.isActiveAt(4L, session.getSessionDate())).thenReturn(false);

        assertThatThrownBy(() -> service.mark(markRequest(AttendanceStatus.PRESENT)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Student is not actively enrolled in this classroom");

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    void markRejectsNewAttendanceOnOrAfterStopEffectiveDate() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.SCHEDULED);
        session.setSessionDate(LocalDate.of(2026, 7, 8));
        Student student = student();
        Enrollment enrollment = enrollment(student, session.getClassroom());
        enrollment.setStatus(EnrollmentStatus.STOPPED);
        enrollment.setEndDate(LocalDate.of(2026, 7, 8));

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findEligibleForAttendanceBySessionDate(
                2L,
                session.getSessionDate()
        )).thenReturn(List.of());
        when(attendanceRepository.findBySessionId(1L)).thenReturn(List.of());
        when(attendanceRepository.findBySessionIdAndStudentId(1L, 3L)).thenReturn(Optional.empty());
        when(enrollmentRepository.findByStudentIdAndClassroomIdOrderByStartDateAscIdAsc(3L, 2L))
                .thenReturn(List.of(enrollment));
        when(statusHistoryRepository.isActiveAt(4L, session.getSessionDate())).thenReturn(false);

        assertThatThrownBy(() -> service.mark(markRequest(AttendanceStatus.PRESENT)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Student is not actively enrolled in this classroom");

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    void readinessRemainsReadyWhenOneStudentIsDepleted() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.SCHEDULED);
        Enrollment depleted = enrollment(student(), session.getClassroom());
        mockEligibleEnrollments(session, depleted, 4, 4);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(attendanceRepository.findBySessionIdAndStudentId(1L, 3L)).thenReturn(Optional.empty());

        AttendanceReadinessResponse response = service.checkReadiness(1L);

        assertThat(response.ready()).isTrue();
        assertThat(response.blockedStudents()).hasSize(1);
        assertThat(response.blockedStudents().getFirst().reason())
                .isEqualTo("Hết buổi - cần gia hạn gói trước khi điểm danh");
    }

    @Test
    void rosterIncludesCurrentlyStoppedLearnerForHistoricalActivePeriod() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.COMPLETED);
        Enrollment enrollment = enrollment(student(), session.getClassroom());
        enrollment.setStatus(EnrollmentStatus.STOPPED);
        mockEnrollmentSessions(enrollment, 8, 3);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findEligibleForAttendanceBySessionDate(
                2L,
                session.getSessionDate()
        )).thenReturn(List.of(enrollment));
        when(attendanceRepository.findBySessionId(1L)).thenReturn(List.of());

        var roster = service.getRoster(1L);

        assertThat(roster.students()).hasSize(1);
        assertThat(roster.students().getFirst().enrollmentId()).isEqualTo(enrollment.getId());
    }

    @Test
    void rosterFollowsActiveHistoryAcrossStopAndReactivateGaps() {
        AttendanceService service = newService();
        Enrollment enrollment = enrollment(student(), session(ClassSessionStatus.SCHEDULED).getClassroom());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        mockEnrollmentSessions(enrollment, 8, 2);

        // ACTIVE [01, 09) + ACTIVE [15, null) after stop day 09 / reactivate day 15
        for (LocalDate date : List.of(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 2),
                LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 7, 16)
        )) {
            assertRosterContainsStudentForDate(service, enrollment, date, true);
        }
        for (LocalDate date : List.of(
                LocalDate.of(2026, 7, 9),
                LocalDate.of(2026, 7, 10)
        )) {
            assertRosterContainsStudentForDate(service, enrollment, date, false);
        }
    }

    @Test
    void rosterIncludesExistingAttendanceEvenWhenCurrentlyStoppedAndNotHistoryEligible() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.COMPLETED);
        Student student = student();
        Enrollment enrollment = enrollment(student, session.getClassroom());
        enrollment.setStatus(EnrollmentStatus.STOPPED);
        enrollment.setEndDate(LocalDate.of(2026, 7, 8));
        mockEnrollmentSessions(enrollment, 8, 2);

        Attendance existing = new Attendance();
        existing.setId(10L);
        existing.setSession(session);
        existing.setStudent(student);
        existing.setStatus(AttendanceStatus.PRESENT);
        existing.setValid(true);

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findEligibleForAttendanceBySessionDate(
                2L,
                session.getSessionDate()
        )).thenReturn(List.of());
        when(attendanceRepository.findBySessionId(1L)).thenReturn(List.of(existing));
        when(enrollmentRepository.findByStudentIdAndClassroomIdOrderByStartDateAscIdAsc(3L, 2L))
                .thenReturn(List.of(enrollment));
        when(statusHistoryRepository.isActiveAt(4L, session.getSessionDate())).thenReturn(false);

        var roster = service.getRoster(1L);

        assertThat(roster.students()).hasSize(1);
        assertThat(roster.students().getFirst().studentId()).isEqualTo(3L);
        assertThat(roster.students().getFirst().enrollmentId()).isEqualTo(4L);
    }

    @Test
    void markUpdatesHistoricalAttendanceWithoutReversingUsedSessionsWhenStopped() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.COMPLETED);
        Student student = student();
        Enrollment enrollment = enrollment(student, session.getClassroom());
        enrollment.setStatus(EnrollmentStatus.STOPPED);
        enrollment.setEndDate(LocalDate.of(2026, 7, 8));
        mockEnrollmentSessions(enrollment, 8, 2);

        Attendance existing = new Attendance();
        existing.setId(10L);
        existing.setSession(session);
        existing.setStudent(student);
        existing.setStatus(AttendanceStatus.PRESENT);
        existing.setValid(true);

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findEligibleForAttendanceBySessionDate(
                2L,
                session.getSessionDate()
        )).thenReturn(List.of());
        when(attendanceRepository.findBySessionId(1L)).thenReturn(List.of(existing));
        when(attendanceRepository.findBySessionIdAndStudentId(1L, 3L)).thenReturn(Optional.of(existing));
        when(enrollmentRepository.findByStudentIdAndClassroomIdOrderByStartDateAscIdAsc(3L, 2L))
                .thenReturn(List.of(enrollment));
        when(statusHistoryRepository.isActiveAt(4L, session.getSessionDate())).thenReturn(false);
        when(attendanceRepository.save(existing)).thenReturn(existing);
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        service.mark(markRequest(AttendanceStatus.ABSENT));

        assertThat(existing.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(enrollment.getUsedSessions()).isEqualTo(2);
    }

    @Test
    void rosterReturnsStudentOnceWhenLegacyEnrollmentsOverlap() {
        AttendanceService service = newService();
        ClassSession session = session(ClassSessionStatus.COMPLETED);
        Student student = student();
        Enrollment first = enrollment(student, session.getClassroom());
        Enrollment second = enrollment(student, session.getClassroom());
        second.setId(5L);
        mockEnrollmentSessions(first, 8, 3);
        mockEnrollmentSessions(second, 8, 1);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findEligibleForAttendanceBySessionDate(
                2L,
                session.getSessionDate()
        )).thenReturn(List.of(second, first));
        when(attendanceRepository.findBySessionId(1L)).thenReturn(List.of());

        var roster = service.getRoster(1L);

        assertThat(roster.students()).hasSize(1);
        assertThat(roster.students().getFirst().enrollmentId()).isEqualTo(first.getId());
    }

    private AttendanceService newService() {
        return new AttendanceService(
                attendanceRepository,
                classSessionRepository,
                enrollmentRepository,
                statusHistoryRepository,
                studentRepository,
                makeupCreditRepository,
                new EnrollmentSessionService(statusHistoryRepository),
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
        lenient().when(statusHistoryRepository.isActiveAt(enrollment.getId(), session.getSessionDate()))
                .thenReturn(true);
        when(enrollmentRepository.findEligibleForAttendanceBySessionDate(
                session.getClassroom().getId(),
                session.getSessionDate()
        )).thenReturn(List.of(enrollment));
        lenient().when(attendanceRepository.findBySessionId(session.getId())).thenReturn(List.of());
    }

    private void mockEnrollmentSessions(Enrollment enrollment, int totalSessions, int usedSessions) {
        enrollment.setTotalSessions(totalSessions);
        enrollment.setUsedSessions(usedSessions);
    }

    private void assertRosterContainsStudentForDate(
            AttendanceService service,
            Enrollment enrollment,
            LocalDate sessionDate,
            boolean expectedPresent
    ) {
        ClassSession session = session(ClassSessionStatus.SCHEDULED);
        session.setId(sessionDate.toEpochDay());
        session.setSessionDate(sessionDate);

        when(classSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(enrollmentRepository.findEligibleForAttendanceBySessionDate(2L, sessionDate))
                .thenReturn(isActiveOnStopReactivateTimeline(sessionDate) ? List.of(enrollment) : List.of());
        when(attendanceRepository.findBySessionId(session.getId())).thenReturn(List.of());

        var roster = service.getRoster(session.getId());
        if (expectedPresent) {
            assertThat(roster.students()).extracting(student -> student.studentId()).contains(3L);
        } else {
            assertThat(roster.students()).extracting(student -> student.studentId()).doesNotContain(3L);
        }
    }

    private boolean isActiveOnStopReactivateTimeline(LocalDate sessionDate) {
        LocalDate stopEffectiveDate = LocalDate.of(2026, 7, 9);
        LocalDate reactivateEffectiveDate = LocalDate.of(2026, 7, 15);
        boolean inFirstActive = !sessionDate.isBefore(LocalDate.of(2026, 7, 1))
                && sessionDate.isBefore(stopEffectiveDate);
        boolean inSecondActive = !sessionDate.isBefore(reactivateEffectiveDate);
        return inFirstActive || inSecondActive;
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
