package com.englishcenter.academic.evaluation;

import com.englishcenter.academic.common.AcademicAccessService;
import com.englishcenter.academic.evaluation.dto.ReopenEvaluationRequest;
import com.englishcenter.academic.evaluation.dto.StudentEvaluationResponse;
import com.englishcenter.academic.evaluation.dto.UpsertStudentEvaluationRequest;
import com.englishcenter.academic.report.StudentProgressReportService;
import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.security.SecurityUtils;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentEvaluationService {
    private final StudentEvaluationRepository studentEvaluationRepository;
    private final EvaluationPeriodService evaluationPeriodService;
    private final AcademicAccessService academicAccessService;
    private final StudentProgressReportService studentProgressReportService;

    public StudentEvaluationService(
            StudentEvaluationRepository studentEvaluationRepository,
            EvaluationPeriodService evaluationPeriodService,
            AcademicAccessService academicAccessService,
            @Lazy StudentProgressReportService studentProgressReportService
    ) {
        this.studentEvaluationRepository = studentEvaluationRepository;
        this.evaluationPeriodService = evaluationPeriodService;
        this.academicAccessService = academicAccessService;
        this.studentProgressReportService = studentProgressReportService;
    }

    @Transactional
    public StudentEvaluationResponse upsert(UpsertStudentEvaluationRequest request) {
        academicAccessService.requireManageClassroom(request.classroomId());
        EvaluationPeriod period = evaluationPeriodService.require(request.evaluationPeriodId());
        if (period.getStatus() == EvaluationPeriodStatus.CLOSED) {
            throw new BusinessException("Kỳ đánh giá đã đóng.");
        }
        if (period.getClassroomId() != null && !period.getClassroomId().equals(request.classroomId())) {
            throw new BusinessException("Kỳ đánh giá không áp dụng cho lớp học này.");
        }
        if (!academicAccessService.isEnrolled(request.studentId(), request.classroomId())) {
            throw new BusinessException("Học viên không thuộc lớp học.");
        }

        StudentEvaluation evaluation = studentEvaluationRepository
                .findByEvaluationPeriodIdAndClassroomIdAndStudentId(
                        request.evaluationPeriodId(),
                        request.classroomId(),
                        request.studentId()
                )
                .orElseGet(StudentEvaluation::new);

        if (evaluation.getId() != null && evaluation.getStatus() == StudentEvaluationStatus.FINALIZED) {
            throw new BusinessException("Nhận xét đã chốt; cần ADMIN mở lại trước khi sửa.");
        }

        evaluation.setEvaluationPeriodId(request.evaluationPeriodId());
        evaluation.setClassroomId(request.classroomId());
        evaluation.setStudentId(request.studentId());
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.teacherId() != null) {
            evaluation.setTeacherId(principal.teacherId());
        }
        evaluation.setStrengths(trimToNull(request.strengths()));
        evaluation.setAreasForImprovement(trimToNull(request.areasForImprovement()));
        evaluation.setLearningAttitude(trimToNull(request.learningAttitude()));
        evaluation.setParticipation(trimToNull(request.participation()));
        evaluation.setHomeworkPerformance(trimToNull(request.homeworkPerformance()));
        evaluation.setTeacherComment(trimToNull(request.teacherComment()));
        evaluation.setRecommendation(trimToNull(request.recommendation()));
        evaluation.setInternalNote(trimToNull(request.internalNote()));
        evaluation.setOverallRating(request.overallRating());
        if (evaluation.getStatus() == null) {
            evaluation.setStatus(StudentEvaluationStatus.DRAFT);
        } else if (evaluation.getStatus() == StudentEvaluationStatus.PUBLISHED) {
            // keep published; content update allowed until finalized
        } else {
            evaluation.setStatus(StudentEvaluationStatus.DRAFT);
        }
        return toResponse(studentEvaluationRepository.save(evaluation), true);
    }

    @Transactional
    public StudentEvaluationResponse publish(Long id) {
        StudentEvaluation evaluation = require(id);
        academicAccessService.requireManageClassroom(evaluation.getClassroomId());
        if (evaluation.getStatus() == StudentEvaluationStatus.FINALIZED) {
            throw new BusinessException("Nhận xét đã chốt không thể xuất bản lại.");
        }
        evaluation.setStatus(StudentEvaluationStatus.PUBLISHED);
        evaluation.setPublishedAt(LocalDateTime.now());
        StudentEvaluation saved = studentEvaluationRepository.save(evaluation);
        studentProgressReportService.createOrSyncOnPublish(saved);
        return toResponse(saved, true);
    }

    @Transactional
    public StudentEvaluationResponse finalize(Long id) {
        StudentEvaluation evaluation = require(id);
        academicAccessService.requireManageClassroom(evaluation.getClassroomId());
        if (evaluation.getStatus() != StudentEvaluationStatus.PUBLISHED
                && evaluation.getStatus() != StudentEvaluationStatus.DRAFT) {
            throw new BusinessException("Chỉ có thể chốt nhận xét ở trạng thái nháp hoặc đã xuất bản.");
        }
        if (evaluation.getStatus() == StudentEvaluationStatus.DRAFT) {
            evaluation.setPublishedAt(LocalDateTime.now());
        }
        evaluation.setStatus(StudentEvaluationStatus.FINALIZED);
        evaluation.setFinalizedAt(LocalDateTime.now());
        StudentEvaluation saved = studentEvaluationRepository.save(evaluation);
        studentProgressReportService.createOrSyncOnFinalize(saved);
        return toResponse(saved, true);
    }

    @Transactional
    public StudentEvaluationResponse reopen(Long id, ReopenEvaluationRequest request) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() != AccountRole.ADMIN) {
            throw new AccessDeniedException("Chỉ ADMIN mới được mở lại nhận xét đã chốt.");
        }
        StudentEvaluation evaluation = require(id);
        if (evaluation.getStatus() != StudentEvaluationStatus.FINALIZED) {
            throw new BusinessException("Chỉ có thể mở lại nhận xét đã chốt.");
        }
        evaluation.setStatus(StudentEvaluationStatus.PUBLISHED);
        evaluation.setFinalizedAt(null);
        evaluation.setReopenReason(request.reason().trim());
        StudentEvaluation saved = studentEvaluationRepository.save(evaluation);
        studentProgressReportService.syncStatus(saved);
        return toResponse(saved, true);
    }

    @Transactional(readOnly = true)
    public StudentEvaluationResponse getById(Long id) {
        StudentEvaluation evaluation = require(id);
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.STUDENT) {
            if (!principal.studentId().equals(evaluation.getStudentId())
                    || (evaluation.getStatus() != StudentEvaluationStatus.PUBLISHED
                    && evaluation.getStatus() != StudentEvaluationStatus.FINALIZED)) {
                throw new NotFoundException("Không tìm thấy nhận xét.");
            }
            return toResponse(evaluation, false);
        }
        academicAccessService.requireAccessClassroom(evaluation.getClassroomId());
        return toResponse(evaluation, true);
    }

    @Transactional(readOnly = true)
    public List<StudentEvaluationResponse> list(Long periodId, Long classroomId) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.STUDENT) {
            throw new BusinessException("Học viên vui lòng dùng API /api/me/evaluations.");
        }
        if (classroomId == null || periodId == null) {
            throw new BusinessException("Vui lòng chọn lớp học và kỳ đánh giá.");
        }
        academicAccessService.requireAccessClassroom(classroomId);
        return studentEvaluationRepository
                .findByEvaluationPeriodIdAndClassroomIdOrderByStudentIdAsc(periodId, classroomId)
                .stream()
                .map(evaluation -> toResponse(evaluation, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentEvaluationResponse> listForCurrentStudent() {
        Long studentId = SecurityUtils.requireLinkedStudentId();
        return studentEvaluationRepository.findByStudentIdOrderByCreatedAtDescIdDesc(studentId).stream()
                .filter(evaluation -> evaluation.getStatus() == StudentEvaluationStatus.PUBLISHED
                        || evaluation.getStatus() == StudentEvaluationStatus.FINALIZED)
                .map(evaluation -> toResponse(evaluation, false))
                .toList();
    }

    private StudentEvaluation require(Long id) {
        return studentEvaluationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhận xét học viên."));
    }

    private StudentEvaluationResponse toResponse(StudentEvaluation evaluation, boolean includeInternal) {
        return new StudentEvaluationResponse(
                evaluation.getId(),
                evaluation.getEvaluationPeriodId(),
                evaluation.getClassroomId(),
                evaluation.getStudentId(),
                evaluation.getTeacherId(),
                evaluation.getStrengths(),
                evaluation.getAreasForImprovement(),
                evaluation.getLearningAttitude(),
                evaluation.getParticipation(),
                evaluation.getHomeworkPerformance(),
                evaluation.getTeacherComment(),
                evaluation.getRecommendation(),
                includeInternal ? evaluation.getInternalNote() : null,
                evaluation.getOverallRating(),
                evaluation.getStatus(),
                evaluation.getPublishedAt(),
                evaluation.getFinalizedAt(),
                includeInternal ? evaluation.getReopenReason() : null,
                evaluation.getCreatedAt(),
                evaluation.getUpdatedAt()
        );
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
