package com.englishcenter.academic.report;

import com.englishcenter.academic.report.dto.ProgressReportDocumentResponse;
import com.englishcenter.academic.report.dto.ProgressReportResponse;
import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academic/progress-reports")
public class StudentProgressReportController {
    private final StudentProgressReportService studentProgressReportService;

    public StudentProgressReportController(StudentProgressReportService studentProgressReportService) {
        this.studentProgressReportService = studentProgressReportService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<ProgressReportResponse>> search(
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long periodId,
            @RequestParam(required = false) ProgressReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ProgressReportResponse> result =
                studentProgressReportService.search(classroomId, studentId, periodId, status, page, size);
        return ApiResponse.success(
                result.getContent(),
                new PageMeta(result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages())
        );
    }

    @GetMapping("/{id}/document")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ApiResponse<ProgressReportDocumentResponse> getDocument(@PathVariable Long id) {
        return ApiResponse.success(studentProgressReportService.getDocument(id));
    }
}
