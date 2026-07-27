package com.englishcenter.academic.material;

import com.englishcenter.academic.assessment.AssessmentRepository;
import com.englishcenter.academic.assignment.AssignmentRepository;
import com.englishcenter.academic.common.AcademicAccessService;
import com.englishcenter.academic.lesson.LessonRecordRepository;
import com.englishcenter.academic.material.dto.LearningMaterialResponse;
import com.englishcenter.academic.storage.FileStorageService;
import com.englishcenter.academic.storage.StoredFile;
import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.security.SecurityUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LearningMaterialService {
    private final LearningMaterialRepository learningMaterialRepository;
    private final ClassroomRepository classroomRepository;
    private final LessonRecordRepository lessonRecordRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicAccessService academicAccessService;
    private final FileStorageService fileStorageService;

    public LearningMaterialService(
            LearningMaterialRepository learningMaterialRepository,
            ClassroomRepository classroomRepository,
            LessonRecordRepository lessonRecordRepository,
            AssignmentRepository assignmentRepository,
            AssessmentRepository assessmentRepository,
            EnrollmentRepository enrollmentRepository,
            AcademicAccessService academicAccessService,
            FileStorageService fileStorageService
    ) {
        this.learningMaterialRepository = learningMaterialRepository;
        this.classroomRepository = classroomRepository;
        this.lessonRecordRepository = lessonRecordRepository;
        this.assignmentRepository = assignmentRepository;
        this.assessmentRepository = assessmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.academicAccessService = academicAccessService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public LearningMaterialResponse upload(
            MultipartFile file,
            String title,
            String description,
            Long classroomId,
            Long lessonRecordId,
            Long assignmentId,
            Long assessmentId,
            MaterialVisibility visibility
    ) {
        if (classroomId == null) {
            throw new BusinessException("Lớp học là bắt buộc.");
        }
        if (!classroomRepository.existsById(classroomId)) {
            throw new NotFoundException("Không tìm thấy lớp học.");
        }
        academicAccessService.requireManageClassroom(classroomId);
        if (title == null || title.isBlank()) {
            throw new BusinessException("Tiêu đề tài liệu là bắt buộc.");
        }
        MaterialVisibility resolvedVisibility = visibility == null ? MaterialVisibility.CLASS_STUDENTS : visibility;
        validateOptionalLinks(classroomId, lessonRecordId, assignmentId, assessmentId);

        StoredFile stored = fileStorageService.store(file, "materials");
        LearningMaterial material = new LearningMaterial();
        material.setTitle(title.trim());
        material.setDescription(trimToNull(description));
        material.setFileName(stored.originalFileName());
        material.setStoredFileName(stored.storedFileName());
        material.setContentType(stored.contentType());
        material.setFileSize(stored.fileSize());
        material.setStorageKey(stored.storageKey());
        material.setClassroomId(classroomId);
        material.setLessonRecordId(lessonRecordId);
        material.setAssignmentId(assignmentId);
        material.setAssessmentId(assessmentId);
        material.setVisibility(resolvedVisibility);
        material.setActive(true);
        material.setUploadedBy(SecurityUtils.currentUsernameOrSystem());
        return toResponse(learningMaterialRepository.save(material));
    }

    @Transactional(readOnly = true)
    public List<LearningMaterialResponse> list(Long classroomId, MaterialVisibility visibility) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.STUDENT) {
            throw new BusinessException("Học viên vui lòng dùng API /api/me/materials.");
        }
        if (classroomId == null) {
            throw new BusinessException("Vui lòng chọn lớp học.");
        }
        academicAccessService.requireAccessClassroom(classroomId);
        return learningMaterialRepository.findActiveByClassroomId(classroomId, visibility).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LearningMaterialResponse> listForCurrentStudent() {
        Long studentId = SecurityUtils.requireLinkedStudentId();
        Map<Long, LearningMaterial> unique = new LinkedHashMap<>();
        enrollmentRepository.findByStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE).forEach(enrollment -> {
            learningMaterialRepository
                    .findActiveByClassroomId(enrollment.getClassroom().getId(), MaterialVisibility.CLASS_STUDENTS)
                    .forEach(material -> unique.putIfAbsent(material.getId(), material));
        });
        // also include any enrolled (not only ACTIVE) classrooms with CLASS_STUDENTS materials
        enrollmentRepository.findByStudentIdOrderByStartDateDescIdDesc(studentId).forEach(enrollment -> {
            learningMaterialRepository
                    .findActiveByClassroomId(enrollment.getClassroom().getId(), MaterialVisibility.CLASS_STUDENTS)
                    .forEach(material -> unique.putIfAbsent(material.getId(), material));
        });
        return unique.values().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DownloadPayload download(Long id) {
        LearningMaterial material = requireActive(id);
        requireDownloadAccess(material);
        Resource resource = fileStorageService.loadAsResource(material.getStorageKey());
        return new DownloadPayload(resource, material.getFileName(), material.getContentType());
    }

    @Transactional
    public void softDelete(Long id) {
        LearningMaterial material = requireActive(id);
        academicAccessService.requireManageClassroom(material.getClassroomId());
        material.setActive(false);
        learningMaterialRepository.save(material);
    }

    private void requireDownloadAccess(LearningMaterial material) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.ADMIN) {
            return;
        }
        if (principal.role() == AccountRole.TEACHER) {
            academicAccessService.requireAccessClassroom(material.getClassroomId());
            return;
        }
        if (principal.role() == AccountRole.STUDENT) {
            if (material.getVisibility() != MaterialVisibility.CLASS_STUDENTS) {
                throw new AccessDeniedException("Bạn không có quyền tải tài liệu này.");
            }
            academicAccessService.requireAccessClassroom(material.getClassroomId());
            return;
        }
        throw new AccessDeniedException("Bạn không có quyền tải tài liệu này.");
    }

    private void validateOptionalLinks(
            Long classroomId,
            Long lessonRecordId,
            Long assignmentId,
            Long assessmentId
    ) {
        if (lessonRecordId != null) {
            var lesson = lessonRecordRepository.findById(lessonRecordId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy nội dung bài học."));
            Long lessonClassroomId = academicAccessService.classroomIdOfSession(lesson.getClassSessionId());
            if (!classroomId.equals(lessonClassroomId)) {
                throw new BusinessException("Nội dung bài học không thuộc lớp học đã chọn.");
            }
        }
        if (assignmentId != null) {
            var assignment = assignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy bài tập."));
            if (!classroomId.equals(assignment.getClassroomId())) {
                throw new BusinessException("Bài tập không thuộc lớp học đã chọn.");
            }
        }
        if (assessmentId != null) {
            var assessment = assessmentRepository.findById(assessmentId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra."));
            if (!classroomId.equals(assessment.getClassroomId())) {
                throw new BusinessException("Bài kiểm tra không thuộc lớp học đã chọn.");
            }
        }
    }

    private LearningMaterial requireActive(Long id) {
        LearningMaterial material = learningMaterialRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài liệu."));
        if (!Boolean.TRUE.equals(material.getActive())) {
            throw new NotFoundException("Không tìm thấy tài liệu.");
        }
        return material;
    }

    private LearningMaterialResponse toResponse(LearningMaterial material) {
        return new LearningMaterialResponse(
                material.getId(),
                material.getTitle(),
                material.getDescription(),
                material.getFileName(),
                material.getContentType(),
                material.getFileSize(),
                material.getClassroomId(),
                material.getLessonRecordId(),
                material.getAssignmentId(),
                material.getAssessmentId(),
                material.getVisibility(),
                material.getActive(),
                material.getUploadedBy(),
                material.getUploadedAt()
        );
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record DownloadPayload(Resource resource, String fileName, String contentType) {
    }
}
