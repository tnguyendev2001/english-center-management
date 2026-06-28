package com.englishcenter.studentpackage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.makeupcredit.MakeupCreditRepository;
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import com.englishcenter.student.Student;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.studentpackage.LearningProgressWarningType;
import com.englishcenter.studentpackage.dto.StudentPackageProgressResponse;
import com.englishcenter.studentpackage.mapper.StudentPackageMapper;
import com.englishcenter.tuitionpackage.TuitionPackage;
import com.englishcenter.tuitionpackage.TuitionPackageStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentPackageServiceTest {
    @Mock
    private StudentPackageRepository studentPackageRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private MakeupCreditRepository makeupCreditRepository;

    private final StudentPackageMapper studentPackageMapper = new StudentPackageMapper();

    @Test
    void getByClassroomIdReturnsActivePackageProgressOnly() {
        StudentPackageService service = newService();
        StudentPackage activePackage = studentPackage(21L, tuitionPackage(4L, "12 sessions", 12, "700000"));

        when(classroomRepository.existsById(2L)).thenReturn(true);
        when(studentPackageRepository.findByClassroomIdAndStatusOrderByStartDateDesc(
                2L,
                StudentPackageStatus.ACTIVE
        )).thenReturn(List.of(activePackage));
        when(attendanceRepository.countUsedSessions(
                1L,
                2L,
                activePackage.getStartDate(),
                activePackage.getEndDate(),
                ClassSessionStatus.CANCELED
        )).thenReturn(0L);
        when(makeupCreditRepository.sumAvailableMakeupSessions(1L, 2L, MakeupCreditStatus.AVAILABLE))
                .thenReturn(0);

        List<StudentPackageProgressResponse> progress = service.getByClassroomId(2L);

        assertThat(progress).hasSize(1);
        assertThat(progress.getFirst().id()).isEqualTo(21L);
        assertThat(progress.getFirst().packageName()).isEqualTo("12 sessions");
        assertThat(progress.getFirst().price()).isEqualByComparingTo("700000");
        assertThat(progress.getFirst().totalSessions()).isEqualTo(12);
        assertThat(progress.getFirst().remainingSessions()).isEqualTo(12);
        assertThat(progress.getFirst().overusedSessions()).isZero();
        assertThat(progress.getFirst().warningType()).isEqualTo(LearningProgressWarningType.NONE);
        assertThat(progress.getFirst().status()).isEqualTo(StudentPackageStatus.ACTIVE);
    }

    @Test
    void getByClassroomIdCalculatesOverusedSessionsAndWarning() {
        StudentPackageService service = newService();
        StudentPackage activePackage = studentPackage(21L, tuitionPackage(4L, "8 sessions", 8, "500000"));

        when(classroomRepository.existsById(2L)).thenReturn(true);
        when(studentPackageRepository.findByClassroomIdAndStatusOrderByStartDateDesc(
                2L,
                StudentPackageStatus.ACTIVE
        )).thenReturn(List.of(activePackage));
        when(attendanceRepository.countUsedSessions(
                1L,
                2L,
                activePackage.getStartDate(),
                activePackage.getEndDate(),
                ClassSessionStatus.CANCELED
        )).thenReturn(11L);
        when(makeupCreditRepository.sumAvailableMakeupSessions(1L, 2L, MakeupCreditStatus.AVAILABLE))
                .thenReturn(0);

        List<StudentPackageProgressResponse> progress = service.getByClassroomId(2L);

        assertThat(progress.getFirst().usedSessions()).isEqualTo(11);
        assertThat(progress.getFirst().remainingSessions()).isZero();
        assertThat(progress.getFirst().overusedSessions()).isEqualTo(3);
        assertThat(progress.getFirst().totalAvailableSessions()).isZero();
        assertThat(progress.getFirst().warningType()).isEqualTo(LearningProgressWarningType.OVERUSED);
        assertThat(progress.getFirst().warningMessage()).isEqualTo("Vượt 3 buổi - cần gia hạn");
    }

    private StudentPackageService newService() {
        return new StudentPackageService(
                studentPackageRepository,
                studentRepository,
                classroomRepository,
                attendanceRepository,
                makeupCreditRepository,
                studentPackageMapper
        );
    }

    private StudentPackage studentPackage(Long id, TuitionPackage tuitionPackage) {
        Student student = new Student();
        student.setId(1L);
        student.setFullName("Nguyen Van A");

        Classroom classroom = new Classroom();
        classroom.setId(2L);
        classroom.setClassName("Starter A");

        Enrollment enrollment = new Enrollment();
        enrollment.setId(10L);

        StudentPackage studentPackage = new StudentPackage();
        studentPackage.setId(id);
        studentPackage.setStudent(student);
        studentPackage.setClassroom(classroom);
        studentPackage.setEnrollment(enrollment);
        studentPackage.setTuitionPackage(tuitionPackage);
        studentPackage.setPackageName(tuitionPackage.getName());
        studentPackage.setTotalSessions(tuitionPackage.getTotalSessions());
        studentPackage.setPrice(tuitionPackage.getPrice());
        studentPackage.setDiscountAmount(BigDecimal.ZERO);
        studentPackage.setAdjustmentAmount(BigDecimal.ZERO);
        studentPackage.setFinalAmount(tuitionPackage.getPrice());
        studentPackage.setStartDate(LocalDate.of(2026, 7, 1));
        studentPackage.setStatus(StudentPackageStatus.ACTIVE);
        studentPackage.setCycleNo(2);
        return studentPackage;
    }

    private TuitionPackage tuitionPackage(Long id, String name, int totalSessions, String price) {
        TuitionPackage tuitionPackage = new TuitionPackage();
        tuitionPackage.setId(id);
        tuitionPackage.setName(name);
        tuitionPackage.setTotalSessions(totalSessions);
        tuitionPackage.setPrice(new BigDecimal(price));
        tuitionPackage.setStatus(TuitionPackageStatus.ACTIVE);
        return tuitionPackage;
    }
}
