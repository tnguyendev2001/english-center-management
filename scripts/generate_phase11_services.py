#!/usr/bin/env python3
"""Generate Phase 11 academic services, DTOs, controllers, auth + alerts."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "backend/src/main/java/com/englishcenter"


def w(rel: str, content: str) -> None:
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.strip() + "\n", encoding="utf-8")
    print(f"wrote {path.relative_to(ROOT)}")


# ===================== MATERIAL =====================

w(
    "academic/material/dto/LearningMaterialResponse.java",
    r'''
package com.englishcenter.academic.material.dto;

import com.englishcenter.academic.material.MaterialVisibility;
import java.time.LocalDateTime;

public record LearningMaterialResponse(
        Long id,
        String title,
        String description,
        String fileName,
        String contentType,
        Long fileSize,
        Long classroomId,
        Long lessonRecordId,
        Long assignmentId,
        Long assessmentId,
        MaterialVisibility visibility,
        Boolean active,
        String uploadedBy,
        LocalDateTime uploadedAt
) {
}
''',
)

w(
    "academic/material/LearningMaterialService.java",
    r'''
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
import java.util.ArrayList;
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
''',
)

w(
    "academic/material/LearningMaterialController.java",
    r'''
package com.englishcenter.academic.material;

import com.englishcenter.academic.material.dto.LearningMaterialResponse;
import com.englishcenter.common.api.ApiResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/academic/materials")
public class LearningMaterialController {
    private final LearningMaterialService learningMaterialService;

    public LearningMaterialController(LearningMaterialService learningMaterialService) {
        this.learningMaterialService = learningMaterialService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<LearningMaterialResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam Long classroomId,
            @RequestParam(required = false) Long lessonRecordId,
            @RequestParam(required = false) Long assignmentId,
            @RequestParam(required = false) Long assessmentId,
            @RequestParam(required = false) MaterialVisibility visibility
    ) {
        return ApiResponse.success(learningMaterialService.upload(
                file, title, description, classroomId, lessonRecordId, assignmentId, assessmentId, visibility
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<LearningMaterialResponse>> list(
            @RequestParam Long classroomId,
            @RequestParam(required = false) MaterialVisibility visibility
    ) {
        return ApiResponse.success(learningMaterialService.list(classroomId, visibility));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        LearningMaterialService.DownloadPayload payload = learningMaterialService.download(id);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(payload.contentType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(payload.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaType)
                .body(payload.resource());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        learningMaterialService.softDelete(id);
        return ApiResponse.success(null);
    }
}
''',
)

print("material done")
