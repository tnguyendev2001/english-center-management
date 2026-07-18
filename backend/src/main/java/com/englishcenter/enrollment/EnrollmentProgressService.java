package com.englishcenter.enrollment;

import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.enrollment.dto.EnrollmentLearningProgressResponse;
import com.englishcenter.enrollment.mapper.EnrollmentLearningProgressMapper;
import com.englishcenter.makeupcredit.MakeupCreditRepository;
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.studentpackage.StudentPackage;
import com.englishcenter.studentpackage.StudentPackageRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentProgressService {
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final StudentPackageRepository studentPackageRepository;
    private final MakeupCreditRepository makeupCreditRepository;
    private final EnrollmentLearningProgressMapper enrollmentLearningProgressMapper;

    public EnrollmentProgressService(
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            ClassroomRepository classroomRepository,
            StudentPackageRepository studentPackageRepository,
            MakeupCreditRepository makeupCreditRepository,
            EnrollmentLearningProgressMapper enrollmentLearningProgressMapper
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.classroomRepository = classroomRepository;
        this.studentPackageRepository = studentPackageRepository;
        this.makeupCreditRepository = makeupCreditRepository;
        this.enrollmentLearningProgressMapper = enrollmentLearningProgressMapper;
    }

    @Transactional(readOnly = true)
    public List<EnrollmentLearningProgressResponse> getByStudentId(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new NotFoundException("Student not found");
        }

        return enrollmentRepository.findByStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE)
                .stream()
                .map(this::toProgressResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentLearningProgressResponse> getByClassroomId(Long classroomId) {
        if (!classroomRepository.existsById(classroomId)) {
            throw new NotFoundException("Classroom not found");
        }

        return enrollmentRepository.findByClassroomIdAndStatus(classroomId, EnrollmentStatus.ACTIVE)
                .stream()
                .map(this::toProgressResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnrollmentLearningProgressResponse getByEnrollmentId(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found"));
        return toProgressResponse(enrollment);
    }

    private EnrollmentLearningProgressResponse toProgressResponse(Enrollment enrollment) {
        StudentPackage latestStudentPackage = studentPackageRepository
                .findTopByEnrollmentIdOrderByCycleNoDescIdDesc(enrollment.getId())
                .orElse(null);
        int makeupAvailableSessions = makeupCreditRepository.countAvailableMakeupCredits(
                enrollment.getStudent().getId(),
                enrollment.getClassroom().getId(),
                MakeupCreditStatus.AVAILABLE
        );

        return enrollmentLearningProgressMapper.toResponse(
                enrollment,
                latestStudentPackage,
                makeupAvailableSessions
        );
    }
}
