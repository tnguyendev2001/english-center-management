package com.englishcenter.enrollment;

import static org.assertj.core.api.Assertions.assertThat;

import com.englishcenter.attendance.Attendance;
import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.attendance.AttendanceStatus;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.student.Student;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentSessionServiceTest {
    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private EnrollmentStatusHistoryRepository statusHistoryRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Test
    void presentToExcusedDecrementsUsedAndIncreasesRemaining() {
        EnrollmentSessionService service = newService();
        Enrollment enrollment = enrollment(8, 3);
        ClassSession session = session();
        Attendance existing = attendance(session, AttendanceStatus.PRESENT);

        service.applyAttendanceDelta(enrollment, existing, session, AttendanceStatus.EXCUSED);

        assertThat(enrollment.getUsedSessions()).isEqualTo(2);
        assertThat(service.remainingSessions(enrollment)).isEqualTo(6);
    }

    @Test
    void excusedToAbsentIncrementsUsedAndDecreasesRemaining() {
        EnrollmentSessionService service = newService();
        Enrollment enrollment = enrollment(8, 2);
        ClassSession session = session();
        Attendance existing = attendance(session, AttendanceStatus.EXCUSED);

        service.applyAttendanceDelta(enrollment, existing, session, AttendanceStatus.ABSENT);

        assertThat(enrollment.getUsedSessions()).isEqualTo(3);
        assertThat(service.remainingSessions(enrollment)).isEqualTo(5);
    }

    @Test
    void reverseConsumedSessionRestoresRemaining() {
        EnrollmentSessionService service = newService();
        Enrollment enrollment = enrollment(8, 3);

        service.reverseConsumedSession(enrollment);

        assertThat(enrollment.getUsedSessions()).isEqualTo(2);
        assertThat(service.remainingSessions(enrollment)).isEqualTo(6);
    }

    private EnrollmentSessionService newService() {
        return new EnrollmentSessionService(
                new EnrollmentEligibilityService(
                        enrollmentRepository,
                        statusHistoryRepository,
                        attendanceRepository,
                        classSessionRepository
                ),
                enrollmentRepository,
                attendanceRepository
        );
    }

    private Enrollment enrollment(int totalSessions, int usedSessions) {
        Student student = new Student();
        student.setId(3L);
        Classroom classroom = new Classroom();
        classroom.setId(2L);

        Enrollment enrollment = new Enrollment();
        enrollment.setId(4L);
        enrollment.setStudent(student);
        enrollment.setClassroom(classroom);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setStartDate(LocalDate.of(2026, 6, 1));
        enrollment.setTotalSessions(totalSessions);
        enrollment.setUsedSessions(usedSessions);
        return enrollment;
    }

    private ClassSession session() {
        Classroom classroom = new Classroom();
        classroom.setId(2L);
        ClassSession session = new ClassSession();
        session.setId(1L);
        session.setClassroom(classroom);
        session.setSessionDate(LocalDate.of(2026, 7, 1));
        session.setStatus(ClassSessionStatus.COMPLETED);
        return session;
    }

    private Attendance attendance(ClassSession session, AttendanceStatus status) {
        Attendance attendance = new Attendance();
        attendance.setId(10L);
        attendance.setSession(session);
        attendance.setStatus(status);
        attendance.setValid(true);
        return attendance;
    }
}
