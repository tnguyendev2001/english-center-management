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
