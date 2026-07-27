package com.englishcenter.classsession;

import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.classsession.dto.CancelClassSessionRequest;
import com.englishcenter.classsession.dto.ClassSessionResponse;
import com.englishcenter.classsession.dto.ClassSessionSearchResponse;
import com.englishcenter.classsession.dto.CreateClassSessionRequest;
import com.englishcenter.classsession.dto.GenerateClassSessionsRequest;
import com.englishcenter.classsession.dto.GenerateClassSessionsResponse;
import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import com.englishcenter.security.SecurityUtils;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClassSessionController {
    private final ClassSessionService classSessionService;
    private final com.englishcenter.security.AuthorizationService authorizationService;

    public ClassSessionController(
            ClassSessionService classSessionService,
            com.englishcenter.security.AuthorizationService authorizationService
    ) {
        this.classSessionService = classSessionService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/api/class-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("""
            hasRole('ADMIN')
            or (
                hasRole('TEACHER')
                and @authorizationService.canManageClassroom(authentication, #request.classroomId())
            )
            """)
    public ApiResponse<ClassSessionResponse> create(@Valid @RequestBody CreateClassSessionRequest request) {
        return ApiResponse.success(classSessionService.create(request));
    }

    @PostMapping("/api/class-sessions/generate")
    @PreAuthorize("""
            hasRole('ADMIN')
            or (
                hasRole('TEACHER')
                and @authorizationService.canManageClassroom(authentication, #request.classroomId())
            )
            """)
    public ApiResponse<GenerateClassSessionsResponse> generate(
            @Valid @RequestBody GenerateClassSessionsRequest request
    ) {
        return ApiResponse.success(classSessionService.generate(request));
    }

    @GetMapping("/api/class-sessions")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
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
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.TEACHER) {
            if (classroomId == null
                    || !authorizationService.canManageClassroom(
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication(),
                    classroomId
            )) {
                throw new AccessDeniedException("Bạn không có quyền thực hiện chức năng này.");
            }
        }

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
    @PreAuthorize("@authorizationService.canAccessSession(authentication, #id)")
    public ApiResponse<ClassSessionResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(classSessionService.getById(id));
    }

    @PostMapping("/api/class-sessions/{id}/cancel")
    @PreAuthorize("@authorizationService.canManageSession(authentication, #id)")
    public ApiResponse<ClassSessionResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelClassSessionRequest request
    ) {
        return ApiResponse.success(classSessionService.cancel(id, request));
    }

    @PostMapping("/api/class-sessions/{id}/correction-cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ClassSessionResponse> correctionCancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelClassSessionRequest request
    ) {
        return ApiResponse.success(classSessionService.correctionCancel(id, request));
    }

    @PostMapping("/api/class-sessions/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ClassSessionResponse> restore(@PathVariable Long id) {
        return ApiResponse.success(classSessionService.restore(id));
    }
}
