package com.englishcenter.teacher;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import com.englishcenter.teacher.dto.TeacherCreateRequest;
import com.englishcenter.teacher.dto.TeacherResponse;
import com.englishcenter.teacher.dto.TeacherUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {
    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<TeacherResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<TeacherResponse> teachers = teacherService.search(keyword, page, size);
        PageMeta meta = new PageMeta(
                teachers.getNumber(),
                teachers.getSize(),
                teachers.getTotalElements(),
                teachers.getTotalPages()
        );
        return ApiResponse.success(teachers.getContent(), meta);
    }

    @GetMapping("/without-account")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<TeacherResponse>> withoutAccount() {
        return ApiResponse.success(teacherService.listWithoutUserAccount());
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<TeacherResponse>> active() {
        return ApiResponse.success(teacherService.listActive());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.canAccessOwnTeacherProfile(authentication, #id)")
    public ApiResponse<TeacherResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(teacherService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TeacherResponse> create(@Valid @RequestBody TeacherCreateRequest request) {
        return ApiResponse.success(teacherService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TeacherResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TeacherUpdateRequest request
    ) {
        return ApiResponse.success(teacherService.update(id, request));
    }
}
