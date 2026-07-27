package com.englishcenter.classroom;

import com.englishcenter.classroom.dto.AssignTeacherRequest;
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
import com.englishcenter.teacher.Teacher;
import com.englishcenter.teacher.TeacherRepository;
import com.englishcenter.teacher.TeacherStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassroomService {
    private static final Logger log = LoggerFactory.getLogger(ClassroomService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final ClassroomRepository classroomRepository;
    private final ClassroomMapper classroomMapper;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentSessionService enrollmentSessionService;
    private final ClassroomScheduleUpdateService classroomScheduleUpdateService;
    private final TeacherRepository teacherRepository;

    public ClassroomService(
            ClassroomRepository classroomRepository,
            ClassroomMapper classroomMapper,
            EnrollmentRepository enrollmentRepository,
            EnrollmentSessionService enrollmentSessionService,
            ClassroomScheduleUpdateService classroomScheduleUpdateService,
            TeacherRepository teacherRepository
    ) {
        this.classroomRepository = classroomRepository;
        this.classroomMapper = classroomMapper;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentSessionService = enrollmentSessionService;
        this.classroomScheduleUpdateService = classroomScheduleUpdateService;
        this.teacherRepository = teacherRepository;
    }

    @Transactional(readOnly = true)
    public Page<ClassroomResponse> search(
            String keyword,
            Long teacherId,
            Boolean unassignedOnly,
            Boolean assignedOnly,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        boolean onlyUnassigned = Boolean.TRUE.equals(unassignedOnly);
        boolean onlyAssigned = Boolean.TRUE.equals(assignedOnly);
        String normalizedKeyword = isBlank(keyword) ? null : keyword.trim();
        Page<Classroom> classrooms;

        if (normalizedKeyword != null || teacherId != null || onlyUnassigned || onlyAssigned) {
            classrooms = classroomRepository.searchFiltered(
                    normalizedKeyword,
                    teacherId,
                    onlyUnassigned,
                    onlyAssigned,
                    pageable
            );
        } else {
            classrooms = classroomRepository.findAll(pageable);
        }

        return classrooms.map(this::toResponseWithLearningProgressWarnings);
    }

    @Transactional(readOnly = true)
    public ClassroomResponse getById(Long id) {
        return toResponseWithLearningProgressWarnings(findClassroom(id));
    }

    @Transactional(readOnly = true)
    public List<ClassroomResponse> listByTeacherId(Long teacherId) {
        return classroomRepository.findByTeacherIdOrderByClassNameAsc(teacherId).stream()
                .map(this::toResponseWithLearningProgressWarnings)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClassroomResponse> listUnassigned() {
        return classroomRepository.findByTeacherIdIsNullOrderByClassNameAsc().stream()
                .map(this::toResponseWithLearningProgressWarnings)
                .toList();
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

        Teacher teacher = requireAssignableTeacher(request.teacherId(), request.status());
        Classroom classroom = classroomMapper.toEntity(request);
        classroomMapper.applyTeacher(classroom, teacher);
        return toResponseWithLearningProgressWarnings(classroomRepository.save(classroom));
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

        Long previousTeacherId = classroom.getTeacherId();
        Teacher teacher = requireAssignableTeacher(request.teacherId(), request.status());
        classroomMapper.updateEntity(classroom, request);
        classroomMapper.applyTeacher(classroom, teacher);
        Classroom saved = classroomRepository.save(classroom);

        if (previousTeacherId == null
                ? saved.getTeacherId() != null
                : !previousTeacherId.equals(saved.getTeacherId())) {
            log.info(
                    "AUTH_AUDIT event=CLASSROOM_TEACHER_CHANGED classroomId={} fromTeacherId={} toTeacherId={}",
                    saved.getId(),
                    previousTeacherId,
                    saved.getTeacherId()
            );
        }

        return toResponseWithLearningProgressWarnings(saved);
    }

    @Transactional
    public ClassroomResponse assignTeacher(Long classroomId, AssignTeacherRequest request) {
        Classroom classroom = findClassroom(classroomId);
        Teacher teacher = requireActiveTeacher(request.teacherId());
        Long previousTeacherId = classroom.getTeacherId();
        classroomMapper.applyTeacher(classroom, teacher);
        Classroom saved = classroomRepository.save(classroom);
        log.info(
                "AUTH_AUDIT event=CLASSROOM_TEACHER_ASSIGNED classroomId={} fromTeacherId={} toTeacherId={}",
                saved.getId(),
                previousTeacherId,
                saved.getTeacherId()
        );
        return toResponseWithLearningProgressWarnings(saved);
    }

    private Teacher requireAssignableTeacher(Long teacherId, ClassroomStatus status) {
        boolean requiresTeacher = status == ClassroomStatus.PLANNED || status == ClassroomStatus.ONGOING;
        if (teacherId == null) {
            if (requiresTeacher) {
                throw new BusinessException("Vui lòng chọn giáo viên phụ trách lớp.");
            }
            return null;
        }
        return requireActiveTeacher(teacherId);
    }

    private Teacher requireActiveTeacher(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException("Giáo viên không hợp lệ."));
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new BusinessException("Chỉ được phân công giáo viên đang hoạt động.");
        }
        return teacher;
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

        return classroomMapper.toResponse(
                classroom,
                overusedCount,
                outOfSessionsCount,
                lowSessionsCount,
                enrollments.size()
        );
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
