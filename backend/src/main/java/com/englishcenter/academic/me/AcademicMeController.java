package com.englishcenter.academic.me;

import com.englishcenter.academic.assessment.AssessmentService;
import com.englishcenter.academic.assessment.dto.AssessmentResponse;
import com.englishcenter.academic.assignment.AssignmentService;
import com.englishcenter.academic.assignment.dto.AssignmentResponse;
import com.englishcenter.academic.evaluation.StudentEvaluationService;
import com.englishcenter.academic.evaluation.dto.StudentEvaluationResponse;
import com.englishcenter.academic.lesson.LessonRecordService;
import com.englishcenter.academic.lesson.dto.LessonRecordResponse;
import com.englishcenter.academic.material.LearningMaterialService;
import com.englishcenter.academic.material.dto.LearningMaterialResponse;
import com.englishcenter.academic.report.StudentProgressReportService;
import com.englishcenter.academic.report.dto.ProgressReportDocumentResponse;
import com.englishcenter.academic.report.dto.ProgressReportResponse;
import com.englishcenter.academic.score.AssessmentScoreService;
import com.englishcenter.academic.score.dto.AssessmentScoreResponse;
import com.englishcenter.academic.submission.AssignmentSubmissionService;
import com.englishcenter.academic.submission.dto.AssignmentSubmissionResponse;
import com.englishcenter.common.api.ApiResponse;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me")
@PreAuthorize("hasRole('STUDENT')")
public class AcademicMeController {
    private final LessonRecordService lessonRecordService;
    private final LearningMaterialService learningMaterialService;
    private final AssignmentService assignmentService;
    private final AssignmentSubmissionService assignmentSubmissionService;
    private final AssessmentService assessmentService;
    private final AssessmentScoreService assessmentScoreService;
    private final StudentEvaluationService studentEvaluationService;
    private final StudentProgressReportService studentProgressReportService;

    public AcademicMeController(
            LessonRecordService lessonRecordService,
            LearningMaterialService learningMaterialService,
            AssignmentService assignmentService,
            AssignmentSubmissionService assignmentSubmissionService,
            AssessmentService assessmentService,
            AssessmentScoreService assessmentScoreService,
            StudentEvaluationService studentEvaluationService,
            StudentProgressReportService studentProgressReportService
    ) {
        this.lessonRecordService = lessonRecordService;
        this.learningMaterialService = learningMaterialService;
        this.assignmentService = assignmentService;
        this.assignmentSubmissionService = assignmentSubmissionService;
        this.assessmentService = assessmentService;
        this.assessmentScoreService = assessmentScoreService;
        this.studentEvaluationService = studentEvaluationService;
        this.studentProgressReportService = studentProgressReportService;
    }

    @GetMapping("/lessons")
    public ApiResponse<List<LessonRecordResponse>> lessons() {
        return ApiResponse.success(lessonRecordService.listForCurrentStudent());
    }

    @GetMapping("/materials")
    public ApiResponse<List<LearningMaterialResponse>> materials() {
        return ApiResponse.success(learningMaterialService.listForCurrentStudent());
    }

    @GetMapping("/assignments")
    public ApiResponse<List<AssignmentResponse>> assignments() {
        return ApiResponse.success(assignmentService.listForCurrentStudent());
    }

    @GetMapping("/assignments/{id}")
    public ApiResponse<AssignmentResponse> assignment(@PathVariable Long id) {
        return ApiResponse.success(assignmentService.getForCurrentStudent(id));
    }

    @PostMapping(value = "/assignments/{id}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AssignmentSubmissionResponse> submit(
            @PathVariable Long id,
            @RequestParam(required = false) String textAnswer,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        return ApiResponse.success(assignmentSubmissionService.submitForCurrentStudent(id, textAnswer, files));
    }

    @GetMapping("/assessments")
    public ApiResponse<List<AssessmentResponse>> assessments() {
        return ApiResponse.success(assessmentService.listForCurrentStudent());
    }

    @GetMapping("/scores")
    public ApiResponse<List<AssessmentScoreResponse>> scores() {
        return ApiResponse.success(assessmentScoreService.listForCurrentStudent());
    }

    @GetMapping("/evaluations")
    public ApiResponse<List<StudentEvaluationResponse>> evaluations() {
        return ApiResponse.success(studentEvaluationService.listForCurrentStudent());
    }

    @GetMapping("/progress-reports")
    public ApiResponse<List<ProgressReportResponse>> progressReports() {
        return ApiResponse.success(studentProgressReportService.listForCurrentStudent());
    }

    @GetMapping("/progress-reports/{id}")
    public ApiResponse<ProgressReportDocumentResponse> progressReport(@PathVariable Long id) {
        return ApiResponse.success(studentProgressReportService.getDocumentForCurrentStudent(id));
    }
}
