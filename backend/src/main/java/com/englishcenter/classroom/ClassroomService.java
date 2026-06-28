package com.englishcenter.classroom;

import com.englishcenter.classroom.dto.ClassroomCreateRequest;
import com.englishcenter.classroom.dto.ClassroomResponse;
import com.englishcenter.classroom.dto.ClassroomUpdateRequest;
import com.englishcenter.classroom.mapper.ClassroomMapper;
import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.studentpackage.StudentPackage;
import com.englishcenter.studentpackage.StudentPackageRepository;
import com.englishcenter.studentpackage.StudentPackageStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassroomService {
    private static final int MAX_PAGE_SIZE = 100;

    private final ClassroomRepository classroomRepository;
    private final ClassroomMapper classroomMapper;
    private final StudentPackageRepository studentPackageRepository;
    private final AttendanceRepository attendanceRepository;

    public ClassroomService(
            ClassroomRepository classroomRepository,
            ClassroomMapper classroomMapper,
            StudentPackageRepository studentPackageRepository,
            AttendanceRepository attendanceRepository
    ) {
        this.classroomRepository = classroomRepository;
        this.classroomMapper = classroomMapper;
        this.studentPackageRepository = studentPackageRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @Transactional(readOnly = true)
    public Page<ClassroomResponse> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Classroom> classrooms = isBlank(keyword)
                ? classroomRepository.findAll(pageable)
                : classroomRepository.search(keyword.trim(), pageable);

        return classrooms.map(this::toResponseWithLearningProgressWarnings);
    }

    @Transactional(readOnly = true)
    public ClassroomResponse getById(Long id) {
        return toResponseWithLearningProgressWarnings(findClassroom(id));
    }

    @Transactional
    public ClassroomResponse create(ClassroomCreateRequest request) {
        String classCode = request.classCode().trim();
        if (classroomRepository.existsByClassCode(classCode)) {
            throw new BusinessException("Class code already exists");
        }

        validateSchedule(request.startDate(), request.expectedEndDate(), request.startTime(), request.endTime());

        Classroom classroom = classroomMapper.toEntity(request);
        return classroomMapper.toResponse(classroomRepository.save(classroom));
    }

    @Transactional
    public ClassroomResponse update(Long id, ClassroomUpdateRequest request) {
        Classroom classroom = findClassroom(id);
        String classCode = request.classCode().trim();

        if (classroomRepository.existsByClassCodeAndIdNot(classCode, id)) {
            throw new BusinessException("Class code already exists");
        }

        validateSchedule(request.startDate(), request.expectedEndDate(), request.startTime(), request.endTime());

        classroomMapper.updateEntity(classroom, request);
        return classroomMapper.toResponse(classroomRepository.save(classroom));
    }

    private Classroom findClassroom(Long id) {
        return classroomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Classroom not found"));
    }

    private ClassroomResponse toResponseWithLearningProgressWarnings(Classroom classroom) {
        int overusedCount = 0;
        int outOfSessionsCount = 0;
        int lowSessionsCount = 0;

        List<StudentPackage> activePackages = studentPackageRepository.findByClassroomIdAndStatusOrderByStartDateDesc(
                classroom.getId(),
                StudentPackageStatus.ACTIVE
        );

        for (StudentPackage studentPackage : activePackages) {
            int usedSessions = Math.toIntExact(attendanceRepository.countUsedSessions(
                    studentPackage.getStudent().getId(),
                    studentPackage.getClassroom().getId(),
                    studentPackage.getStartDate(),
                    studentPackage.getEndDate(),
                    ClassSessionStatus.CANCELED
            ));
            int remainingSessions = Math.max(studentPackage.getTotalSessions() - usedSessions, 0);
            int overusedSessions = Math.max(usedSessions - studentPackage.getTotalSessions(), 0);

            if (overusedSessions > 0) {
                overusedCount++;
            } else if (remainingSessions == 0) {
                outOfSessionsCount++;
            } else if (remainingSessions <= 2) {
                lowSessionsCount++;
            }
        }

        return classroomMapper.toResponse(classroom, overusedCount, outOfSessionsCount, lowSessionsCount);
    }

    private void validateSchedule(
            LocalDate startDate,
            LocalDate expectedEndDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        if (expectedEndDate != null && startDate != null && expectedEndDate.isBefore(startDate)) {
            throw new BusinessException("Expected end date must not be before start date");
        }

        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new BusinessException("End time must be after start time");
        }
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
