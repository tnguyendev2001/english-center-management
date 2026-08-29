package com.englishcenter.studentpackage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentSessionService;
import com.englishcenter.student.Student;
import com.englishcenter.studentpackage.dto.StudentPackagePeriodResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PackageTimelineServiceTest {
    @Mock
    private StudentPackageRepository studentPackageRepository;
    @Mock
    private StudentPackagePeriodAdjustmentRepository adjustmentRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private ClassSessionRepository classSessionRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private EnrollmentEligibilityService enrollmentEligibilityService;
    @Mock
    private EnrollmentSessionService enrollmentSessionService;

    private PackageTimelineService service;

    @BeforeEach
    void setUp() {
        service = new PackageTimelineService(
                studentPackageRepository,
                adjustmentRepository,
                enrollmentRepository,
                classSessionRepository,
                attendanceRepository,
                enrollmentEligibilityService,
                enrollmentSessionService
        );
    }

    @Test
    void historicalExcusedCorrectionMovesCycleEndAndNextCycleStart() {
        Enrollment enrollment = enrollment();
        StudentPackage cycle1 = studentPackage(1L, enrollment, 1);
        StudentPackage cycle2 = studentPackage(2L, enrollment, 2);
        List<Attendance> attendances = attendances(
                LocalDate.of(2026, 8, 7),
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 14),
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 21),
                LocalDate.of(2026, 8, 24),
                LocalDate.of(2026, 8, 28),
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 4),
                LocalDate.of(2026, 9, 7)
        );
        stubTimeline(enrollment, List.of(cycle1, cycle2), attendances);

        List<StudentPackagePeriodResponse> initial = service.recalculatePackageTimeline(30L);

        assertThat(initial.get(0).calculatedPeriodStartDate()).isEqualTo(LocalDate.of(2026, 8, 7));
        assertThat(initial.get(0).calculatedPeriodEndDate()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(initial.get(1).calculatedPeriodStartDate()).isEqualTo(LocalDate.of(2026, 9, 4));

        attendances.get(6).setStatus(AttendanceStatus.EXCUSED);
        List<StudentPackagePeriodResponse> corrected = service.recalculatePackageTimeline(30L);

        assertThat(corrected.get(0).calculatedPeriodStartDate()).isEqualTo(LocalDate.of(2026, 8, 7));
        assertThat(corrected.get(0).calculatedPeriodEndDate()).isEqualTo(LocalDate.of(2026, 9, 4));
        assertThat(corrected.get(1).calculatedPeriodStartDate()).isEqualTo(LocalDate.of(2026, 9, 7));
        assertThat(corrected.get(1).calculatedPeriodEndDate()).isNull();
        assertThat(cycle1.getFinalAmount()).isEqualByComparingTo("500000");
        assertThat(cycle2.getFinalAmount()).isEqualByComparingTo("500000");
    }

    @Test
    void manualStartPreservesCalculatedValueAndWritesAuditHistory() {
        Enrollment enrollment = enrollment();
        StudentPackage cycle = studentPackage(1L, enrollment, 1);
        cycle.setCalculatedPeriodStartDate(LocalDate.of(2026, 8, 7));
        cycle.setCalculatedPeriodEndDate(LocalDate.of(2026, 8, 31));
        ClassSession selectedSession = new ClassSession();
        selectedSession.setSessionDate(LocalDate.of(2026, 8, 10));
        selectedSession.setStatus(ClassSessionStatus.COMPLETED);

        when(studentPackageRepository.findWithRelationsForUpdateById(1L)).thenReturn(Optional.of(cycle));
        when(classSessionRepository.findByClassroomIdAndSessionDateOrderByStartTimeAscIdAsc(
                20L,
                LocalDate.of(2026, 8, 10)
        )).thenReturn(List.of(selectedSession));
        when(enrollmentEligibilityService.isActiveOnDate(enrollment, LocalDate.of(2026, 8, 10)))
                .thenReturn(true);
        when(studentPackageRepository.findByEnrollmentIdOrderByCycleNoAscIdAsc(30L))
                .thenReturn(List.of(cycle));
        when(adjustmentRepository.save(any(StudentPackagePeriodAdjustment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentPackagePeriodResponse response = service.adjustPeriodStart(
                1L,
                LocalDate.of(2026, 8, 10),
                "Điều chỉnh theo hồ sơ lớp",
                "admin"
        );

        assertThat(response.calculatedPeriodStartDate()).isEqualTo(LocalDate.of(2026, 8, 7));
        assertThat(response.effectivePeriodStartDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(response.manualOverrideReason()).isEqualTo("Điều chỉnh theo hồ sơ lớp");
        assertThat(response.manualOverrideChangedBy()).isEqualTo("admin");
    }

    private void stubTimeline(
            Enrollment enrollment,
            List<StudentPackage> packages,
            List<Attendance> attendances
    ) {
        when(enrollmentRepository.findById(30L)).thenReturn(Optional.of(enrollment));
        when(studentPackageRepository.findByEnrollmentIdOrderByCycleNoAscIdAsc(30L)).thenReturn(packages);
        when(attendanceRepository.findValidByStudentIdAndClassroomId(10L, 20L)).thenReturn(attendances);
        when(enrollmentSessionService.consumesSession(
                any(Attendance.class),
                any(ClassSession.class),
                any(Enrollment.class)
        )).thenAnswer(invocation -> {
            Attendance attendance = invocation.getArgument(0);
            return attendance.getStatus() != AttendanceStatus.EXCUSED;
        });
        for (StudentPackage studentPackage : packages) {
            when(adjustmentRepository.findTopByStudentPackageIdOrderByChangedAtDescIdDesc(studentPackage.getId()))
                    .thenReturn(Optional.empty());
        }
    }

    private Enrollment enrollment() {
        Student student = new Student();
        student.setId(10L);
        Classroom classroom = new Classroom();
        classroom.setId(20L);
        Enrollment enrollment = new Enrollment();
        enrollment.setId(30L);
        enrollment.setStudent(student);
        enrollment.setClassroom(classroom);
        enrollment.setStartDate(LocalDate.of(2026, 8, 7));
        return enrollment;
    }

    private StudentPackage studentPackage(Long id, Enrollment enrollment, int cycleNo) {
        StudentPackage studentPackage = new StudentPackage();
        studentPackage.setId(id);
        studentPackage.setEnrollment(enrollment);
        studentPackage.setStudent(enrollment.getStudent());
        studentPackage.setClassroom(enrollment.getClassroom());
        studentPackage.setCycleNo(cycleNo);
        studentPackage.setPackageName("Gói 8 buổi");
        studentPackage.setTotalSessions(8);
        studentPackage.setFinalAmount(new BigDecimal("500000"));
        studentPackage.setPeriodNeedsRecalculation(true);
        return studentPackage;
    }

    private List<Attendance> attendances(LocalDate... dates) {
        List<Attendance> attendances = new ArrayList<>();
        for (int index = 0; index < dates.length; index++) {
            ClassSession session = new ClassSession();
            session.setId((long) index + 1);
            session.setSessionDate(dates[index]);
            session.setStartTime(LocalTime.of(18, 0));
            session.setStatus(ClassSessionStatus.COMPLETED);

            Attendance attendance = new Attendance();
            attendance.setSession(session);
            attendance.setStatus(AttendanceStatus.PRESENT);
            attendance.setValid(true);
            attendances.add(attendance);
        }
        return attendances;
    }
}
