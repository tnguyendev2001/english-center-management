package com.englishcenter.financial;

import com.englishcenter.classroom.Classroom;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class StudentCurrentClassroomResolver {
    private final EnrollmentRepository enrollmentRepository;

    public StudentCurrentClassroomResolver(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    public Map<Long, Classroom> resolve(Collection<Long> studentIds) {
        Map<Long, Classroom> classroomsByStudentId = new LinkedHashMap<>();
        if (studentIds == null || studentIds.isEmpty()) {
            return classroomsByStudentId;
        }

        List<Enrollment> enrollments = enrollmentRepository.findCurrentClassroomCandidates(studentIds);
        for (Enrollment enrollment : enrollments) {
            classroomsByStudentId.putIfAbsent(enrollment.getStudent().getId(), enrollment.getClassroom());
        }
        return classroomsByStudentId;
    }
}
