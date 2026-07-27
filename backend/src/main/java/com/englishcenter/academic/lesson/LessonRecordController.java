package com.englishcenter.academic.lesson;

import com.englishcenter.academic.lesson.dto.CreateLessonRecordRequest;
import com.englishcenter.academic.lesson.dto.LessonRecordResponse;
import com.englishcenter.academic.lesson.dto.UpdateLessonRecordRequest;
import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academic/lessons")
public class LessonRecordController {
    private final LessonRecordService lessonRecordService;

    public LessonRecordController(LessonRecordService lessonRecordService) {
        this.lessonRecordService = lessonRecordService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<LessonRecordResponse>> search(
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) LessonStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<LessonRecordResponse> result =
                lessonRecordService.search(classroomId, status, keyword, page, size);
        return ApiResponse.success(
                result.getContent(),
                new PageMeta(
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages()
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ApiResponse<LessonRecordResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(lessonRecordService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<LessonRecordResponse> create(@Valid @RequestBody CreateLessonRecordRequest request) {
        return ApiResponse.success(lessonRecordService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<LessonRecordResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLessonRecordRequest request
    ) {
        return ApiResponse.success(lessonRecordService.update(id, request));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<LessonRecordResponse> publish(@PathVariable Long id) {
        return ApiResponse.success(lessonRecordService.publish(id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<LessonRecordResponse> complete(@PathVariable Long id) {
        return ApiResponse.success(lessonRecordService.complete(id));
    }
}
