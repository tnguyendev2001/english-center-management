package com.englishcenter.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.englishcenter.attendance.Attendance;
import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.attendance.AttendanceStatus;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentEligibilityService;
import com.englishcenter.enrollment.EnrollmentSessionService;
import com.englishcenter.student.Student;
import com.englishcenter.studentpackage.StudentPackage;
import com.englishcenter.studentpackage.StudentPackageRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TuitionPeriodResolverTest {
    @Mock
    private ClassSessionRepository classSessionRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private StudentPackageRepository studentPackageRepository;
    @Mock
    private EnrollmentEligibilityService enrollmentEligibilityService;
    @Mock
    private EnrollmentSessionService enrollmentSessionService;

    private TuitionPeriodResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TuitionPeriodResolver(
                classSessionRepository,
                attendanceRepository,
                studentPackageRepository,
                enrollmentEligibilityService,
                enrollmentSessionService
        );
    }

    @Test
    void extendsPeriodWhenExcusedSessionDoesNotConsumePackage() {
        Invoice invoice = invoice(8);
        List<ClassSession> sessions = sessionsFromAugust(3, 6, 10, 13, 17, 20, 24, 27, 31);
        Attendance excused = attendance(sessions.get(2), AttendanceStatus.EXCUSED);

        stubCommon(invoice, sessions, List.of(excused));
        when(enrollmentSessionService.consumesSession(excused, sessions.get(2), invoice.getEnrollment()))
                .thenReturn(false);

        TuitionPeriodResolver.TuitionPeriod period = resolver.resolve(invoice);

        assertThat(period.start()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(period.end()).isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @Test
    void resolvesEightKnownSessionsToExpectedPackageEnd() {
        Invoice invoice = invoice(8);
        List<ClassSession> sessions = sessionsFromAugust(3, 6, 10, 13, 17, 20, 24, 27);
        stubCommon(invoice, sessions, List.of());

        TuitionPeriodResolver.TuitionPeriod period = resolver.resolve(invoice);

        assertThat(period.start()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(period.end()).isEqualTo(LocalDate.of(2026, 8, 27));
    }

    @Test
    void leavesEndNullWhenNotEnoughKnownSessionsExist() {
        Invoice invoice = invoice(8);
        List<ClassSession> sessions = sessionsFromAugust(3, 6, 10);
        stubCommon(invoice, sessions, List.of());

        TuitionPeriodResolver.TuitionPeriod period = resolver.resolve(invoice);

        assertThat(period.start()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(period.end()).isNull();
    }

    @Test
    void stopsOldCycleBeforeNextRenewalCycle() {
        Invoice invoice = invoice(8);
        StudentPackage nextPackage = new StudentPackage();
        nextPackage.setId(2L);
        nextPackage.setEnrollment(invoice.getEnrollment());
        nextPackage.setCycleNo(2);
        nextPackage.setStartDate(LocalDate.of(2026, 8, 17));
        List<ClassSession> sessions = sessionsFromAugust(3, 6, 10, 13, 17, 20, 24, 27);

        when(studentPackageRepository.findAllByEnrollmentId(30L))
                .thenReturn(List.of(invoice.getStudentPackage(), nextPackage));
        when(attendanceRepository.findValidByStudentIdAndClassroomId(10L, 20L)).thenReturn(List.of());
        when(classSessionRepository.findByClassroomIdOrderBySessionDateAscStartTimeAsc(20L))
                .thenReturn(sessions);
        when(enrollmentEligibilityService.isActiveOnDate(
                org.mockito.ArgumentMatchers.eq(invoice.getEnrollment()),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(true);

        TuitionPeriodResolver.TuitionPeriod period = resolver.resolve(invoice);

        assertThat(period.start()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(period.end()).isNull();
    }

    private void stubCommon(Invoice invoice, List<ClassSession> sessions, List<Attendance> attendances) {
        when(studentPackageRepository.findAllByEnrollmentId(30L)).thenReturn(List.of(invoice.getStudentPackage()));
        when(attendanceRepository.findValidByStudentIdAndClassroomId(10L, 20L)).thenReturn(attendances);
        when(classSessionRepository.findByClassroomIdOrderBySessionDateAscStartTimeAsc(20L))
                .thenReturn(sessions);
        when(enrollmentEligibilityService.isActiveOnDate(
                org.mockito.ArgumentMatchers.eq(invoice.getEnrollment()),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(true);
    }

    private Invoice invoice(int sessionCount) {
        Student student = new Student();
        student.setId(10L);

        Classroom classroom = new Classroom();
        classroom.setId(20L);

        Enrollment enrollment = new Enrollment();
        enrollment.setId(30L);
        enrollment.setStudent(student);
        enrollment.setClassroom(classroom);
        enrollment.setStartDate(LocalDate.of(2026, 8, 3));

        StudentPackage studentPackage = new StudentPackage();
        studentPackage.setId(1L);
        studentPackage.setStudent(student);
        studentPackage.setClassroom(classroom);
        studentPackage.setEnrollment(enrollment);
        studentPackage.setCycleNo(1);
        studentPackage.setTotalSessions(sessionCount);
        studentPackage.setStartDate(LocalDate.of(2026, 8, 3));

        Invoice invoice = new Invoice();
        invoice.setStudent(student);
        invoice.setClassroom(classroom);
        invoice.setEnrollment(enrollment);
        invoice.setStudentPackage(studentPackage);
        invoice.setTotalSessionsSnapshot(sessionCount);
        return invoice;
    }

    private List<ClassSession> sessionsFromAugust(int... days) {
        List<ClassSession> sessions = new ArrayList<>();
        for (int index = 0; index < days.length; index++) {
            ClassSession session = new ClassSession();
            session.setId((long) index + 1);
            session.setSessionDate(LocalDate.of(2026, 8, days[index]));
            session.setStartTime(LocalTime.of(18, 0));
            session.setStatus(ClassSessionStatus.SCHEDULED);
            sessions.add(session);
        }
        return sessions;
    }

    private Attendance attendance(ClassSession session, AttendanceStatus status) {
        Attendance attendance = new Attendance();
        attendance.setSession(session);
        attendance.setStatus(status);
        attendance.setValid(true);
        return attendance;
    }
}
