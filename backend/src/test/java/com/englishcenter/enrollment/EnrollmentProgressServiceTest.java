package com.englishcenter.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.enrollment.dto.EnrollmentLearningProgressResponse;
import com.englishcenter.enrollment.mapper.EnrollmentLearningProgressMapper;
import com.englishcenter.makeupcredit.MakeupCreditRepository;
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import com.englishcenter.student.Student;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.studentpackage.LearningProgressWarningType;
import com.englishcenter.studentpackage.StudentPackage;
import com.englishcenter.studentpackage.StudentPackageRepository;
import com.englishcenter.tuitionpackage.TuitionPackage;
import com.englishcenter.tuitionpackage.TuitionPackageStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentProgressServiceTest {
    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private StudentPackageRepository studentPackageRepository;

    @Mock
    private MakeupCreditRepository makeupCreditRepository;

    private final EnrollmentLearningProgressMapper enrollmentLearningProgressMapper =
            new EnrollmentLearningProgressMapper();

    @Test
    void getByClassroomIdReturnsEnrollmentProgress() {
        EnrollmentProgressService service = newService();
        Enrollment enrollment = enrollment(10L, 12, 0);
        StudentPackage latestPackage = latestPackage(enrollment, tuitionPackage(4L, "12 sessions", 12, "700000"));

        when(classroomRepository.existsById(2L)).thenReturn(true);
        when(enrollmentRepository.findByClassroomIdAndStatus(2L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(enrollment));
        when(studentPackageRepository.findTopByEnrollmentIdOrderByCycleNoDescIdDesc(10L))
                .thenReturn(java.util.Optional.of(latestPackage));
        when(makeupCreditRepository.countAvailableMakeupCredits(1L, 2L, MakeupCreditStatus.AVAILABLE))
                .thenReturn(0);

        List<EnrollmentLearningProgressResponse> progress = service.getByClassroomId(2L);

        assertThat(progress).hasSize(1);
        assertThat(progress.getFirst().enrollmentId()).isEqualTo(10L);
        assertThat(progress.getFirst().latestPackageName()).isEqualTo("12 sessions");
        assertThat(progress.getFirst().latestPackagePrice()).isEqualByComparingTo("700000");
        assertThat(progress.getFirst().totalSessions()).isEqualTo(12);
        assertThat(progress.getFirst().remainingSessions()).isEqualTo(12);
        assertThat(progress.getFirst().overusedSessions()).isZero();
        assertThat(progress.getFirst().warningType()).isEqualTo(LearningProgressWarningType.OK);
    }

    @Test
    void getByClassroomIdCalculatesOverusedSessionsAndWarning() {
        EnrollmentProgressService service = newService();
        Enrollment enrollment = enrollment(10L, 8, 11);
        StudentPackage latestPackage = latestPackage(enrollment, tuitionPackage(4L, "8 sessions", 8, "500000"));

        when(classroomRepository.existsById(2L)).thenReturn(true);
        when(enrollmentRepository.findByClassroomIdAndStatus(2L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(enrollment));
        when(studentPackageRepository.findTopByEnrollmentIdOrderByCycleNoDescIdDesc(10L))
                .thenReturn(java.util.Optional.of(latestPackage));
        when(makeupCreditRepository.countAvailableMakeupCredits(1L, 2L, MakeupCreditStatus.AVAILABLE))
                .thenReturn(0);

        List<EnrollmentLearningProgressResponse> progress = service.getByClassroomId(2L);

        assertThat(progress.getFirst().usedSessions()).isEqualTo(11);
        assertThat(progress.getFirst().remainingSessions()).isZero();
        assertThat(progress.getFirst().overusedSessions()).isEqualTo(3);
        assertThat(progress.getFirst().warningType()).isEqualTo(LearningProgressWarningType.OVERUSED);
        assertThat(progress.getFirst().warningMessage()).isEqualTo("Vượt 3 buổi - cần gia hạn");
    }

    private EnrollmentProgressService newService() {
        return new EnrollmentProgressService(
                enrollmentRepository,
                studentRepository,
                classroomRepository,
                studentPackageRepository,
                makeupCreditRepository,
                enrollmentLearningProgressMapper
        );
    }

    private Enrollment enrollment(Long id, int totalSessions, int usedSessions) {
        Student student = new Student();
        student.setId(1L);
        student.setFullName("Nguyen Van A");

        Classroom classroom = new Classroom();
        classroom.setId(2L);
        classroom.setClassName("Starter A");

        TuitionPackage selectedPackage = tuitionPackage(3L, "12 sessions", 12, "700000");

        Enrollment enrollment = new Enrollment();
        enrollment.setId(id);
        enrollment.setStudent(student);
        enrollment.setClassroom(classroom);
        enrollment.setSelectedPackage(selectedPackage);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setPackageNameSnapshot("12 sessions");
        enrollment.setTotalSessions(totalSessions);
        enrollment.setUsedSessions(usedSessions);
        enrollment.setTotalSessionsSnapshot(12);
        enrollment.setPackagePriceSnapshot(new BigDecimal("700000"));
        return enrollment;
    }

    private StudentPackage latestPackage(Enrollment enrollment, TuitionPackage tuitionPackage) {
        StudentPackage studentPackage = new StudentPackage();
        studentPackage.setId(21L);
        studentPackage.setStudent(enrollment.getStudent());
        studentPackage.setClassroom(enrollment.getClassroom());
        studentPackage.setEnrollment(enrollment);
        studentPackage.setTuitionPackage(tuitionPackage);
        studentPackage.setPackageName(tuitionPackage.getName());
        studentPackage.setTotalSessions(tuitionPackage.getTotalSessions());
        studentPackage.setPrice(tuitionPackage.getPrice());
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
