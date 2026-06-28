package com.englishcenter.studentpackage;

import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.makeupcredit.MakeupCreditRepository;
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.studentpackage.dto.StudentPackageProgressResponse;
import com.englishcenter.studentpackage.mapper.StudentPackageMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentPackageService {
    private final StudentPackageRepository studentPackageRepository;
    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final AttendanceRepository attendanceRepository;
    private final MakeupCreditRepository makeupCreditRepository;
    private final StudentPackageMapper studentPackageMapper;

    public StudentPackageService(
            StudentPackageRepository studentPackageRepository,
            StudentRepository studentRepository,
            ClassroomRepository classroomRepository,
            AttendanceRepository attendanceRepository,
            MakeupCreditRepository makeupCreditRepository,
            StudentPackageMapper studentPackageMapper
    ) {
        this.studentPackageRepository = studentPackageRepository;
        this.studentRepository = studentRepository;
        this.classroomRepository = classroomRepository;
        this.attendanceRepository = attendanceRepository;
        this.makeupCreditRepository = makeupCreditRepository;
        this.studentPackageMapper = studentPackageMapper;
    }

    @Transactional(readOnly = true)
    public List<StudentPackageProgressResponse> getByStudentId(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new NotFoundException("Student not found");
        }

        return studentPackageRepository.findByStudentIdOrderByStartDateDesc(studentId)
                .stream()
                .map(this::toProgressResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentPackageProgressResponse> getByClassroomId(Long classroomId) {
        if (!classroomRepository.existsById(classroomId)) {
            throw new NotFoundException("Classroom not found");
        }

        return studentPackageRepository.findByClassroomIdOrderByStartDateDesc(classroomId)
                .stream()
                .map(this::toProgressResponse)
                .toList();
    }

    private StudentPackageProgressResponse toProgressResponse(StudentPackage studentPackage) {
        Long studentId = studentPackage.getStudent().getId();
        Long classroomId = studentPackage.getClassroom().getId();

        int usedSessions = Math.toIntExact(attendanceRepository.countUsedSessions(
                studentId,
                classroomId,
                studentPackage.getStartDate(),
                studentPackage.getEndDate(),
                ClassSessionStatus.CANCELED
        ));
        int makeupAvailableSessions = makeupCreditRepository.sumAvailableMakeupSessions(
                studentId,
                classroomId,
                MakeupCreditStatus.AVAILABLE
        );

        return studentPackageMapper.toProgressResponse(
                studentPackage,
                usedSessions,
                makeupAvailableSessions
        );
    }
}
