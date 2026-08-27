package com.englishcenter.importdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.englishcenter.attendance.Attendance;
import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.attendance.AttendanceStatus;
import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classroom.ClassroomStatus;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionService;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.classsession.dto.SessionGenerationPlan;
import com.englishcenter.common.config.AppTimeProperties;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentSessionService;
import com.englishcenter.importdata.LegacyExcelWorkbookParser.ParsedRow;
import com.englishcenter.importdata.LegacyExcelWorkbookParser.ParsedSheet;
import com.englishcenter.importdata.dto.LegacyImportRowStatus;
import com.englishcenter.student.Student;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.tuitionpackage.TuitionPackage;
import com.englishcenter.tuitionpackage.TuitionPackageRepository;
import com.englishcenter.tuitionpackage.TuitionPackageStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class LegacyStudentImportServiceTest {
    private static final LocalDate LEARNING_START = LocalDate.of(2026, 6, 5);
    private static final List<LocalDate> EXACT_SESSION_DATES = List.of(
            LocalDate.of(2026, 6, 5),
            LocalDate.of(2026, 6, 9),
            LocalDate.of(2026, 6, 12),
            LocalDate.of(2026, 6, 16),
            LocalDate.of(2026, 6, 19),
            LocalDate.of(2026, 6, 23),
            LocalDate.of(2026, 6, 26),
            LocalDate.of(2026, 6, 30),
            LocalDate.of(2026, 7, 3),
            LocalDate.of(2026, 7, 7),
            LocalDate.of(2026, 7, 10)
    );

    @Mock
    private LegacyExcelWorkbookParser workbookParser;
    @Mock
    private TuitionPackageRepository tuitionPackageRepository;
    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private ClassSessionRepository classSessionRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private ClassSessionService classSessionService;
    @Mock
    private LegacyStudentImportWriter importWriter;

    private LegacyStudentImportService service;
    private MockMultipartFile file;

    @BeforeEach
    void setUp() {
        TuitionPackage tuitionPackage = new TuitionPackage();
        tuitionPackage.setId(8L);
        tuitionPackage.setName("Gói 8 buổi");
        tuitionPackage.setTotalSessions(8);
        tuitionPackage.setPrice(new BigDecimal("1000000"));
        tuitionPackage.setStatus(TuitionPackageStatus.ACTIVE);
        when(tuitionPackageRepository.findById(8L)).thenReturn(Optional.of(tuitionPackage));
        file = new MockMultipartFile("file", "legacy.xlsx", "application/octet-stream", new byte[]{1});

        EnrollmentSessionService enrollmentSessionService =
                new EnrollmentSessionService(null, null, null);
        service = new LegacyStudentImportService(
                workbookParser,
                tuitionPackageRepository,
                classroomRepository,
                studentRepository,
                enrollmentRepository,
                classSessionRepository,
                attendanceRepository,
                classSessionService,
                enrollmentSessionService,
                importWriter,
                new AppTimeProperties()
        );
    }

    @Test
    void exactAttendanceCaseUsesNineConsumingSessionsAndTwoPackages() {
        ParsedRow row = row(
                List.of(LocalDate.of(2026, 6, 12), LocalDate.of(2026, 6, 16)),
                List.of(LocalDate.of(2026, 6, 19), LocalDate.of(2026, 6, 26))
        );
        mockNewClassroom(row);

        var preview = service.preview(file, 8L).rows().getFirst();

        assertThat(preview.eligibleSessionCount()).isEqualTo(11);
        assertThat(preview.presentCount()).isEqualTo(7);
        assertThat(preview.absentCount()).isEqualTo(2);
        assertThat(preview.excusedCount()).isEqualTo(2);
        assertThat(preview.consumingSessionCount()).isEqualTo(9);
        assertThat(preview.packageCycles()).isEqualTo(2);
        assertThat(preview.totalSessionsAfterImport()).isEqualTo(16);
        assertThat(preview.usedSessionsAfterImport()).isEqualTo(9);
        assertThat(preview.remainingSessionsAfterImport()).isEqualTo(7);
        assertThat(preview.unpaidInvoicesToCreate()).isEqualTo(2);
    }

    @Test
    void blankAttendanceColumnsDefaultAllEligibleSessionsToPresent() {
        ParsedRow row = row(List.of(), List.of());
        mockNewClassroom(row);

        var preview = service.preview(file, 8L).rows().getFirst();

        assertThat(preview.presentCount()).isEqualTo(11);
        assertThat(preview.absentCount()).isZero();
        assertThat(preview.excusedCount()).isZero();
        assertThat(preview.consumingSessionCount()).isEqualTo(11);
        assertThat(preview.packageCycles()).isEqualTo(2);
    }

    @Test
    void rejectsOverlapBeforeStartFutureAndNonSessionDates() {
        LocalDate beforeStart = LocalDate.of(2026, 6, 2);
        LocalDate overlap = LocalDate.of(2026, 6, 19);
        LocalDate nonSession = LocalDate.of(2026, 6, 18);
        LocalDate future = LocalDate.now(new AppTimeProperties().zoneId()).plusDays(2);
        ParsedRow row = row(
                List.of(overlap, beforeStart),
                List.of(overlap, nonSession, future)
        );
        mockNewClassroom(row);

        var preview = service.preview(file, 8L);
        var errors = preview.rows().getFirst().errors();

        assertThat(preview.canConfirm()).isFalse();
        assertThat(errors).anyMatch(message -> message.contains("xuất hiện đồng thời"));
        assertThat(errors).anyMatch(message -> message.contains("nằm trước ngày bắt đầu học"));
        assertThat(errors).anyMatch(message -> message.contains("18/06/2026")
                && message.contains("không phải là buổi học hợp lệ"));
        assertThat(errors).anyMatch(message -> message.contains("sau ngày nghiệp vụ hiện tại"));
    }

    @Test
    void existingAttendanceSameStatusIsReusableButDifferentStatusIsConflict() {
        LocalDate sessionDate = LocalDate.of(2026, 6, 19);
        Classroom classroom = existingClassroom();
        Student student = new Student();
        student.setId(3L);
        student.setFullName("Nguyễn Văn Test");
        ClassSession session = existingSession(classroom, sessionDate);
        Enrollment enrollment = new Enrollment();
        enrollment.setId(4L);
        enrollment.setStudent(student);
        enrollment.setClassroom(classroom);
        enrollment.setStartDate(LEARNING_START);
        Attendance attendance = new Attendance();
        attendance.setSession(session);
        attendance.setStudent(student);
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendance.setValid(true);

        ParsedRow row = new ParsedRow(
                5,
                "Nguyễn Văn Test",
                null,
                LEARNING_START,
                null,
                "05/06/2026",
                List.of(),
                List.of(sessionDate),
                List.of(),
                List.of()
        );
        ParsedSheet sheet = sheet(row);
        when(workbookParser.parse(file)).thenReturn(List.of(sheet));
        when(classroomRepository.findFirstByClassCodeIgnoreCase(anyString())).thenReturn(Optional.of(classroom));
        when(classSessionService.planGeneration(classroom, classroom.getStartDate(), businessDate()))
                .thenReturn(new SessionGenerationPlan(
                        classroom.getId(),
                        1,
                        0,
                        1,
                        sessionDate,
                        sessionDate,
                        List.of(sessionDate)
                ));
        when(classSessionRepository.findByClassroomIdOrderBySessionDateAscStartTimeAsc(classroom.getId()))
                .thenReturn(List.of(session));
        when(enrollmentRepository.findByClassroomIdOrderByStartDateDescIdDesc(2L))
                .thenReturn(List.of(enrollment));
        when(enrollmentRepository.findByStudentIdAndClassroomIdOrderByStartDateAscIdAsc(3L, 2L))
                .thenReturn(List.of(enrollment));
        when(attendanceRepository.findBySessionIdAndStudentId(10L, 3L)).thenReturn(Optional.of(attendance));

        var conflict = service.preview(file, 8L);
        assertThat(conflict.canConfirm()).isFalse();
        assertThat(conflict.rows().getFirst().errors())
                .anyMatch(message -> message.contains("PRESENT") && message.contains("EXCUSED"));

        attendance.setStatus(AttendanceStatus.EXCUSED);
        var reusable = service.preview(file, 8L);
        assertThat(reusable.canConfirm()).isTrue();
        assertThat(reusable.rows().getFirst().status())
                .isEqualTo(LegacyImportRowStatus.SKIPPED_DUPLICATE_ENROLLMENT);
    }

    private void mockNewClassroom(ParsedRow row) {
        when(workbookParser.parse(file)).thenReturn(List.of(sheet(row)));
        when(classroomRepository.findFirstByClassCodeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(classroomRepository.findByNormalizedClassName(anyString())).thenReturn(List.of());
        when(classSessionService.plannedSessionDates(
                any(LocalDate.class),
                any(),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(EXACT_SESSION_DATES);
    }

    private ParsedSheet sheet(ParsedRow row) {
        return new ParsedSheet(
                0,
                "TEST CLASS",
                LEARNING_START,
                Set.of(ClassDayOfWeek.TUESDAY, ClassDayOfWeek.FRIDAY),
                List.of(),
                List.of(),
                List.of(row)
        );
    }

    private ParsedRow row(List<LocalDate> absentDates, List<LocalDate> excusedDates) {
        return new ParsedRow(
                5,
                "Nguyễn Văn Test",
                null,
                LEARNING_START,
                null,
                "05/06/2026",
                absentDates,
                excusedDates,
                List.of(),
                List.of()
        );
    }

    private Classroom existingClassroom() {
        Classroom classroom = new Classroom();
        classroom.setId(2L);
        classroom.setClassCode("TESTCLASS");
        classroom.setClassName("TEST CLASS");
        classroom.setStartDate(LEARNING_START);
        classroom.setDaysOfWeek(Set.of(ClassDayOfWeek.TUESDAY, ClassDayOfWeek.FRIDAY));
        classroom.setStartTime(LocalTime.of(17, 0));
        classroom.setEndTime(LocalTime.of(18, 30));
        classroom.setStatus(ClassroomStatus.ONGOING);
        return classroom;
    }

    private ClassSession existingSession(Classroom classroom, LocalDate date) {
        ClassSession session = new ClassSession();
        session.setId(10L);
        session.setClassroom(classroom);
        session.setSessionDate(date);
        session.setStartTime(classroom.getStartTime());
        session.setEndTime(classroom.getEndTime());
        session.setStatus(ClassSessionStatus.COMPLETED);
        return session;
    }

    private LocalDate businessDate() {
        return LocalDate.now(new AppTimeProperties().zoneId());
    }
}
