package com.englishcenter.classroom;

import com.englishcenter.classroom.dto.ClassroomCreateRequest;
import com.englishcenter.classroom.dto.ClassroomResponse;
import com.englishcenter.classroom.dto.ClassroomUpdateRequest;
import com.englishcenter.classroom.mapper.ClassroomMapper;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentSessionService;
import com.englishcenter.enrollment.EnrollmentStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
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
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentSessionService enrollmentSessionService;
    private final ClassroomScheduleUpdateService classroomScheduleUpdateService;

    public ClassroomService(
            ClassroomRepository classroomRepository,
            ClassroomMapper classroomMapper,
            EnrollmentRepository enrollmentRepository,
            EnrollmentSessionService enrollmentSessionService,
            ClassroomScheduleUpdateService classroomScheduleUpdateService
    ) {
        this.classroomRepository = classroomRepository;
        this.classroomMapper = classroomMapper;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentSessionService = enrollmentSessionService;
        this.classroomScheduleUpdateService = classroomScheduleUpdateService;
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

        validateSchedule(
                request.startDate(),
                request.expectedEndDate(),
                request.daysOfWeek(),
                request.startTime(),
                request.endTime()
        );

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

        validateSchedule(
                request.startDate(),
                request.expectedEndDate(),
                request.daysOfWeek(),
                request.startTime(),
                request.endTime()
        );

        LocalDate oldStartDate = classroom.getStartDate();
        Set<ClassDayOfWeek> oldDaysOfWeek = Set.copyOf(classroom.getDaysOfWeek());
        LocalTime oldStartTime = classroom.getStartTime();
        LocalTime oldEndTime = classroom.getEndTime();

        classroomScheduleUpdateService.applyScheduleChangeIfNeeded(
                classroom.getId(),
                oldStartDate,
                oldDaysOfWeek,
                oldStartTime,
                oldEndTime,
                request.startDate(),
                Set.copyOf(request.daysOfWeek()),
                request.startTime(),
                request.endTime()
        );

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

        List<Enrollment> enrollments = enrollmentRepository.findByClassroomIdAndStatus(
                classroom.getId(),
                EnrollmentStatus.ACTIVE
        );

        for (Enrollment enrollment : enrollments) {
            int remainingSessions = enrollmentSessionService.remainingSessions(enrollment);
            int overusedSessions = enrollmentSessionService.overusedSessions(enrollment);

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
            Set<ClassDayOfWeek> daysOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        ClassroomScheduleValidator.validateSchedule(
                startDate,
                expectedEndDate,
                daysOfWeek,
                startTime,
                endTime
        );
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
