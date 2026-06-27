package com.englishcenter.classsession;

import com.englishcenter.classsession.dto.CancelClassSessionRequest;
import com.englishcenter.classsession.dto.ClassSessionResponse;
import com.englishcenter.classsession.dto.GenerateClassSessionsRequest;
import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
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
    public ApiResponse<List<ClassSessionResponse>> generate(
            @Valid @RequestBody GenerateClassSessionsRequest request
    ) {
        return ApiResponse.success(classSessionService.generate(request));
    }

    @GetMapping("/api/class-sessions")
    public ApiResponse<List<ClassSessionResponse>> search(
            @RequestParam(required = false) Long classroomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ClassSessionResponse> sessions = classSessionService.search(classroomId, page, size);
        PageMeta meta = new PageMeta(
                sessions.getNumber(),
                sessions.getSize(),
                sessions.getTotalElements(),
                sessions.getTotalPages()
        );
        return ApiResponse.success(sessions.getContent(), meta);
    }

    @GetMapping("/api/dashboard/today-sessions")
    public ApiResponse<List<ClassSessionResponse>> getTodaySessions() {
        return ApiResponse.success(classSessionService.getTodaySessions());
    }

    @PostMapping("/api/class-sessions/{id}/cancel")
    public ApiResponse<ClassSessionResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelClassSessionRequest request
    ) {
        return ApiResponse.success(classSessionService.cancel(id, request));
    }
}
