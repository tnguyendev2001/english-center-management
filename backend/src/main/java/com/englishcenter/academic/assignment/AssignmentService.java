package com.englishcenter.academic.assignment;

import com.englishcenter.academic.assignment.dto.AssignmentResponse;
import com.englishcenter.academic.assignment.dto.CreateAssignmentRequest;
import com.englishcenter.academic.assignment.dto.UpdateAssignmentRequest;
import com.englishcenter.academic.common.AcademicAccessService;
import com.englishcenter.academic.lesson.LessonRecordRepository;
import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.common.config.AppTimeProperties;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentService {
    private final AssignmentRepository assignmentRepository;
    private final AssignmentTargetRepository assignmentTargetRepository;
    private final ClassroomRepository classroomRepository;
    private final LessonRecordRepository lessonRecordRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicAccessService academicAccessService;
    private final AppTimeProperties appTimeProperties;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            AssignmentTargetRepository assignmentTargetRepository,
            ClassroomRepository classroomRepository,
            LessonRecordRepository lessonRecordRepository,
            EnrollmentRepository enrollmentRepository,
            AcademicAccessService academicAccessService,
            AppTimeProperties appTimeProperties
    ) {
        this.assignmentRepository = assignmentRepository;
        this.assignmentTargetRepository = assignmentTargetRepository;
        this.classroomRepository = classroomRepository;
        this.lessonRecordRepository = lessonRecordRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.academicAccessService = academicAccessService;
        this.appTimeProperties = appTimeProperties;
    }

    @Transactional
    public AssignmentResponse create(CreateAssignmentRequest request) {
        academicAccessService.requireManageClassroom(request.classroomId());
        if (!classroomRepository.existsById(request.classroomId())) {
            throw new NotFoundException("Không tìm thấy lớp học.");
        }
        validateDatesAndScore(request.assignedDate(), request.dueDate(), request.maxScore());
        validateLesson(request.classroomId(), request.lessonRecordId());
        List<Long> targetStudentIds = validateTargets(
                request.classroomId(),
                request.targetMode(),
                request.targetStudentIds()
        );

        Assignment assignment = new Assignment();
        assignment.setTitle(request.title().trim());
        assignment.setDescription(trimToNull(request.description()));
        assignment.setInstructions(trimToNull(request.instructions()));
        assignment.setClassroomId(request.classroomId());
        assignment.setLessonRecordId(request.lessonRecordId());
        assignment.setAssignedDate(request.assignedDate());
        assignment.setDueDate(request.dueDate());
        assignment.setMaxScore(request.maxScore());
        assignment.setAllowSubmission(request.allowSubmission() == null || request.allowSubmission());
        assignment.setAllowLateSubmission(Boolean.TRUE.equals(request.allowLateSubmission()));
        assignment.setTargetMode(request.targetMode());
        assignment.setStatus(AssignmentStatus.DRAFT);
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        assignment.setCreatedByTeacherId(principal.teacherId());
        assignment.setCreatedBy(SecurityUtils.currentUsernameOrSystem());
        Assignment saved = assignmentRepository.save(assignment);
        replaceTargets(saved.getId(), request.targetMode(), targetStudentIds);
        return toResponse(saved);
    }

    @Transactional
    public AssignmentResponse update(Long id, UpdateAssignmentRequest request) {
        Assignment assignment = require(id);
        academicAccessService.requireManageClassroom(assignment.getClassroomId());
        if (assignment.getStatus() == AssignmentStatus.CANCELED) {
            throw new BusinessException("Không thể sửa bài tập đã hủy.");
        }
        if (assignment.getStatus() == AssignmentStatus.CLOSED) {
            throw new BusinessException("Không thể sửa bài tập đã đóng.");
        }
        validateDatesAndScore(request.assignedDate(), request.dueDate(), request.maxScore());
        validateLesson(assignment.getClassroomId(), request.lessonRecordId());
        List<Long> targetStudentIds = validateTargets(
                assignment.getClassroomId(),
                request.targetMode(),
                request.targetStudentIds()
        );

        assignment.setTitle(request.title().trim());
        assignment.setDescription(trimToNull(request.description()));
        assignment.setInstructions(trimToNull(request.instructions()));
        assignment.setLessonRecordId(request.lessonRecordId());
        assignment.setAssignedDate(request.assignedDate());
        assignment.setDueDate(request.dueDate());
        assignment.setMaxScore(request.maxScore());
        if (request.allowSubmission() != null) {
            assignment.setAllowSubmission(request.allowSubmission());
        }
        if (request.allowLateSubmission() != null) {
            assignment.setAllowLateSubmission(request.allowLateSubmission());
        }
        assignment.setTargetMode(request.targetMode());
        Assignment saved = assignmentRepository.save(assignment);
        replaceTargets(saved.getId(), request.targetMode(), targetStudentIds);
        return toResponse(saved);
    }

    @Transactional
    public AssignmentResponse publish(Long id) {
        Assignment assignment = require(id);
        academicAccessService.requireManageClassroom(assignment.getClassroomId());
        if (assignment.getStatus() != AssignmentStatus.DRAFT && assignment.getStatus() != AssignmentStatus.CLOSED) {
            throw new BusinessException("Chỉ có thể xuất bản bài tập ở trạng thái nháp hoặc đã đóng.");
        }
        if (assignment.getTargetMode() == AssignmentTargetMode.SELECTED_STUDENTS
                && assignmentTargetRepository.findStudentIdsByAssignmentId(id).isEmpty()) {
            throw new BusinessException("Bài tập chọn học viên phải có danh sách học viên.");
        }
        assignment.setStatus(AssignmentStatus.PUBLISHED);
        return toResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public AssignmentResponse close(Long id) {
        Assignment assignment = require(id);
        academicAccessService.requireManageClassroom(assignment.getClassroomId());
        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new BusinessException("Chỉ có thể đóng bài tập đang xuất bản.");
        }
        assignment.setStatus(AssignmentStatus.CLOSED);
        return toResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public AssignmentResponse cancel(Long id) {
        Assignment assignment = require(id);
        academicAccessService.requireManageClassroom(assignment.getClassroomId());
        if (assignment.getStatus() == AssignmentStatus.CANCELED) {
            return toResponse(assignment);
        }
        assignment.setStatus(AssignmentStatus.CANCELED);
        return toResponse(assignmentRepository.save(assignment));
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getById(Long id) {
        Assignment assignment = require(id);
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.STUDENT) {
            throw new BusinessException("Học viên vui lòng dùng API /api/me/assignments.");
        }
        academicAccessService.requireAccessClassroom(assignment.getClassroomId());
        return toResponse(assignment);
    }

    @Transactional(readOnly = true)
    public Page<AssignmentResponse> search(
            Long classroomId,
            AssignmentStatus status,
            String keyword,
            int page,
            int size
    ) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        Long teacherId = null;
        if (principal.role() == AccountRole.STUDENT) {
            throw new BusinessException("Học viên vui lòng dùng API /api/me/assignments.");
        }
        if (principal.role() == AccountRole.TEACHER) {
            teacherId = principal.teacherId();
            if (classroomId != null) {
                academicAccessService.requireManageClassroom(classroomId);
            }
        } else if (classroomId != null) {
            academicAccessService.requireAccessClassroom(classroomId);
        }
        return assignmentRepository
                .search(classroomId, status, teacherId, blankToNull(keyword), PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listForCurrentStudent() {
        Long studentId = SecurityUtils.requireLinkedStudentId();
        Map<Long, Assignment> unique = new LinkedHashMap<>();
        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdOrderByStartDateDescIdDesc(studentId);
        for (Enrollment enrollment : enrollments) {
            Long classroomId = enrollment.getClassroom().getId();
            for (AssignmentStatus status : List.of(AssignmentStatus.PUBLISHED, AssignmentStatus.CLOSED)) {
                assignmentRepository.findEligibleForStudent(classroomId, studentId, status)
                        .forEach(assignment -> unique.putIfAbsent(assignment.getId(), assignment));
            }
        }
        return unique.values().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getForCurrentStudent(Long id) {
        Long studentId = SecurityUtils.requireLinkedStudentId();
        Assignment assignment = require(id);
        if (assignment.getStatus() != AssignmentStatus.PUBLISHED && assignment.getStatus() != AssignmentStatus.CLOSED) {
            throw new NotFoundException("Không tìm thấy bài tập.");
        }
        if (!isEligibleStudent(assignment, studentId)) {
            throw new NotFoundException("Không tìm thấy bài tập.");
        }
        return toResponse(assignment);
    }

    public boolean isEligibleStudent(Assignment assignment, Long studentId) {
        if (!academicAccessService.isEnrolled(studentId, assignment.getClassroomId())) {
            return false;
        }
        if (assignment.getTargetMode() == AssignmentTargetMode.ENTIRE_CLASS) {
            return true;
        }
        return assignmentTargetRepository.existsByAssignmentIdAndStudentId(assignment.getId(), studentId);
    }

    public LocalDate today() {
        return LocalDate.now(appTimeProperties.zoneId());
    }

    private void replaceTargets(Long assignmentId, AssignmentTargetMode mode, List<Long> studentIds) {
        assignmentTargetRepository.deleteByAssignmentId(assignmentId);
        if (mode == AssignmentTargetMode.SELECTED_STUDENTS) {
            for (Long studentId : studentIds) {
                AssignmentTarget target = new AssignmentTarget();
                target.setAssignmentId(assignmentId);
                target.setStudentId(studentId);
                assignmentTargetRepository.save(target);
            }
        }
    }

    private List<Long> validateTargets(Long classroomId, AssignmentTargetMode mode, List<Long> targetStudentIds) {
        if (mode == AssignmentTargetMode.ENTIRE_CLASS) {
            return List.of();
        }
        if (targetStudentIds == null || targetStudentIds.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất một học viên.");
        }
        Set<Long> enrolled = enrollmentRepository.findByClassroomIdAndStatus(classroomId, EnrollmentStatus.ACTIVE)
                .stream()
                .map(enrollment -> enrollment.getStudent().getId())
                .collect(Collectors.toSet());
        // also allow any enrolled status
        enrollmentRepository.findByClassroomIdOrderByStartDateDescIdDesc(classroomId).forEach(enrollment ->
                enrolled.add(enrollment.getStudent().getId()));
        List<Long> distinct = targetStudentIds.stream().filter(Objects::nonNull).distinct().toList();
        for (Long studentId : distinct) {
            if (!enrolled.contains(studentId)) {
                throw new BusinessException("Học viên không thuộc lớp học được giao bài tập.");
            }
        }
        return distinct;
    }

    private void validateLesson(Long classroomId, Long lessonRecordId) {
        if (lessonRecordId == null) {
            return;
        }
        var lesson = lessonRecordRepository.findById(lessonRecordId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nội dung bài học."));
        Long lessonClassroomId = academicAccessService.classroomIdOfSession(lesson.getClassSessionId());
        if (!classroomId.equals(lessonClassroomId)) {
            throw new BusinessException("Nội dung bài học không thuộc lớp học đã chọn.");
        }
    }

    private void validateDatesAndScore(LocalDate assignedDate, LocalDate dueDate, BigDecimal maxScore) {
        if (dueDate != null && dueDate.isBefore(assignedDate)) {
            throw new BusinessException("Hạn nộp phải từ ngày giao trở đi.");
        }
        if (maxScore != null && maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Điểm tối đa phải lớn hơn 0.");
        }
    }

    private Assignment require(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài tập."));
    }

    private AssignmentResponse toResponse(Assignment assignment) {
        List<Long> targets = assignment.getTargetMode() == AssignmentTargetMode.SELECTED_STUDENTS
                ? assignmentTargetRepository.findStudentIdsByAssignmentId(assignment.getId())
                : List.of();
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getInstructions(),
                assignment.getClassroomId(),
                assignment.getLessonRecordId(),
                assignment.getAssignedDate(),
                assignment.getDueDate(),
                assignment.getMaxScore(),
                assignment.getAllowSubmission(),
                assignment.getAllowLateSubmission(),
                assignment.getTargetMode(),
                targets,
                assignment.getStatus(),
                assignment.getCreatedByTeacherId(),
                assignment.getCreatedBy(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
