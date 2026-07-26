package com.englishcenter.classsession;

import com.englishcenter.classsession.dto.CancelClassSessionRequest;
import com.englishcenter.classsession.dto.ClassSessionResponse;
import com.englishcenter.classsession.dto.ClassSessionSearchResponse;
import com.englishcenter.classsession.dto.GenerateClassSessionsRequest;
import com.englishcenter.classsession.dto.GenerateClassSessionsResponse;
import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClassSessionController {
    private final ClassSessionService classSessionService;

    public ClassSessionController(ClassSessionService classSessionService) {
        this.classSessionService = classSessionService;
    }

    @PostMapping("/api/class-sessions/generate")
    public ApiResponse<GenerateClassSessionsResponse> generate(
            @Valid @RequestBody GenerateClassSessionsRequest request
    ) {
        return ApiResponse.success(classSessionService.generate(request));
    }

    @GetMapping("/api/class-sessions")
    public ApiResponse<ClassSessionSearchResponse> search(
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) ClassSessionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "sessionDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        ClassSessionService.SearchResult result = classSessionService.search(
                classroomId,
                fromDate,
                toDate,
                status,
                page,
                size,
                sort,
                direction
        );
        PageMeta meta = new PageMeta(
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
        return ApiResponse.success(result.data(), meta);
    }

    @GetMapping("/api/class-sessions/{id}")
    public ApiResponse<ClassSessionResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(classSessionService.getById(id));
    }

    @PostMapping("/api/class-sessions/{id}/cancel")
    public ApiResponse<ClassSessionResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelClassSessionRequest request
    ) {
        return ApiResponse.success(classSessionService.cancel(id, request));
    }

    @PostMapping("/api/class-sessions/{id}/correction-cancel")
    public ApiResponse<ClassSessionResponse> correctionCancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelClassSessionRequest request
    ) {
        return ApiResponse.success(classSessionService.correctionCancel(id, request));
    }

    @PostMapping("/api/class-sessions/{id}/restore")
    public ApiResponse<ClassSessionResponse> restore(@PathVariable Long id) {
        return ApiResponse.success(classSessionService.restore(id));
    }
}
