package com.englishcenter.academic.assessment;

import com.englishcenter.academic.assessment.dto.AssessmentResponse;
import com.englishcenter.academic.assessment.dto.CreateAssessmentRequest;
import com.englishcenter.academic.assessment.dto.UpdateAssessmentRequest;
import com.englishcenter.academic.common.AcademicAccessService;
import com.englishcenter.academic.evaluation.EvaluationPeriodRepository;
import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.security.SecurityUtils;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentService {
    private final AssessmentRepository assessmentRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassSessionRepository classSessionRepository;
    private final EvaluationPeriodRepository evaluationPeriodRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicAccessService academicAccessService;

    public AssessmentService(
            AssessmentRepository assessmentRepository,
            ClassroomRepository classroomRepository,
            ClassSessionRepository classSessionRepository,
            EvaluationPeriodRepository evaluationPeriodRepository,
            EnrollmentRepository enrollmentRepository,
            AcademicAccessService academicAccessService
    ) {
        this.assessmentRepository = assessmentRepository;
        this.classroomRepository = classroomRepository;
        this.classSessionRepository = classSessionRepository;
        this.evaluationPeriodRepository = evaluationPeriodRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.academicAccessService = academicAccessService;
    }

    @Transactional
    public AssessmentResponse create(CreateAssessmentRequest request) {
        academicAccessService.requireManageClassroom(request.classroomId());
        if (!classroomRepository.existsById(request.classroomId())) {
            throw new NotFoundException("Không tìm thấy lớp học.");
        }
        validateScoreAndWeight(request.maxScore(), request.weight());
        validateOptionalLinks(request.classroomId(), request.classSessionId(), request.evaluationPeriodId());

        Assessment assessment = new Assessment();
        assessment.setClassroomId(request.classroomId());
        assessment.setClassSessionId(request.classSessionId());
        assessment.setEvaluationPeriodId(request.evaluationPeriodId());
        assessment.setTitle(request.title().trim());
        assessment.setDescription(trimToNull(request.description()));
        assessment.setType(request.type());
        assessment.setAssessmentDate(request.assessmentDate());
        assessment.setMaxScore(request.maxScore());
        assessment.setWeight(request.weight());
        assessment.setStatus(AssessmentStatus.DRAFT);
        assessment.setPublishedToStudents(false);
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        assessment.setCreatedByTeacherId(principal.teacherId());
        assessment.setCreatedBy(SecurityUtils.currentUsernameOrSystem());
        return toResponse(assessmentRepository.save(assessment));
    }

    @Transactional
    public AssessmentResponse update(Long id, UpdateAssessmentRequest request) {
        Assessment assessment = require(id);
        academicAccessService.requireManageClassroom(assessment.getClassroomId());
        if (assessment.getStatus() == AssessmentStatus.CANCELED) {
            throw new BusinessException("Không thể sửa bài kiểm tra đã hủy.");
        }
        validateScoreAndWeight(request.maxScore(), request.weight());
        validateOptionalLinks(assessment.getClassroomId(), request.classSessionId(), request.evaluationPeriodId());

        assessment.setClassSessionId(request.classSessionId());
        assessment.setEvaluationPeriodId(request.evaluationPeriodId());
        assessment.setTitle(request.title().trim());
        assessment.setDescription(trimToNull(request.description()));
        assessment.setType(request.type());
        assessment.setAssessmentDate(request.assessmentDate());
        assessment.setMaxScore(request.maxScore());
        assessment.setWeight(request.weight());
        return toResponse(assessmentRepository.save(assessment));
    }

    @Transactional
    public AssessmentResponse open(Long id) {
        Assessment assessment = require(id);
        academicAccessService.requireManageClassroom(assessment.getClassroomId());
        if (assessment.getStatus() != AssessmentStatus.DRAFT) {
            throw new BusinessException("Chỉ có thể mở bài kiểm tra ở trạng thái nháp.");
        }
        assessment.setStatus(AssessmentStatus.OPEN);
        return toResponse(assessmentRepository.save(assessment));
    }

    @Transactional
    public AssessmentResponse complete(Long id) {
        Assessment assessment = require(id);
        academicAccessService.requireManageClassroom(assessment.getClassroomId());
        if (assessment.getStatus() != AssessmentStatus.OPEN && assessment.getStatus() != AssessmentStatus.DRAFT) {
            throw new BusinessException("Không thể hoàn thành bài kiểm tra ở trạng thái hiện tại.");
        }
        assessment.setStatus(AssessmentStatus.COMPLETED);
        return toResponse(assessmentRepository.save(assessment));
    }

    @Transactional
    public AssessmentResponse cancel(Long id) {
        Assessment assessment = require(id);
        academicAccessService.requireManageClassroom(assessment.getClassroomId());
        assessment.setStatus(AssessmentStatus.CANCELED);
        return toResponse(assessmentRepository.save(assessment));
    }

    @Transactional(readOnly = true)
    public AssessmentResponse getById(Long id) {
        Assessment assessment = require(id);
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.STUDENT) {
            throw new BusinessException("Học viên vui lòng dùng API /api/me/assessments.");
        }
        academicAccessService.requireAccessClassroom(assessment.getClassroomId());
        return toResponse(assessment);
    }

    @Transactional(readOnly = true)
    public Page<AssessmentResponse> search(
            Long classroomId,
            AssessmentStatus status,
            Long periodId,
            int page,
            int size
    ) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.STUDENT) {
            throw new BusinessException("Học viên vui lòng dùng API /api/me/assessments.");
        }
        if (principal.role() == AccountRole.TEACHER) {
            if (classroomId != null) {
                academicAccessService.requireManageClassroom(classroomId);
            }
            return assessmentRepository
                    .findByTeacherId(principal.teacherId(), status, periodId, PageRequest.of(page, size))
                    .map(this::toResponse);
        }
        if (classroomId != null) {
            academicAccessService.requireAccessClassroom(classroomId);
        }
        return assessmentRepository
                .search(classroomId, status, periodId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> listForCurrentStudent() {
        Long studentId = SecurityUtils.requireLinkedStudentId();
        Map<Long, Assessment> unique = new LinkedHashMap<>();
        for (Enrollment enrollment : enrollmentRepository.findByStudentIdOrderByStartDateDescIdDesc(studentId)) {
            assessmentRepository.findByClassroomIdAndStatusNot(
                            enrollment.getClassroom().getId(),
                            AssessmentStatus.CANCELED
                    )
                    .stream()
                    .filter(assessment -> assessment.getStatus() != AssessmentStatus.DRAFT)
                    .forEach(assessment -> unique.putIfAbsent(assessment.getId(), assessment));
        }
        return unique.values().stream().map(this::toResponse).toList();
    }

    public Assessment require(Long id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra."));
    }

    private void validateOptionalLinks(Long classroomId, Long classSessionId, Long evaluationPeriodId) {
        if (classSessionId != null) {
            if (!classSessionRepository.existsById(classSessionId)) {
                throw new NotFoundException("Không tìm thấy buổi học.");
            }
            Long sessionClassroomId = academicAccessService.classroomIdOfSession(classSessionId);
            if (!classroomId.equals(sessionClassroomId)) {
                throw new BusinessException("Buổi học không thuộc lớp học đã chọn.");
            }
        }
        if (evaluationPeriodId != null) {
            var period = evaluationPeriodRepository.findById(evaluationPeriodId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ đánh giá."));
            if (period.getClassroomId() != null && !period.getClassroomId().equals(classroomId)) {
                throw new BusinessException("Kỳ đánh giá không áp dụng cho lớp học này.");
            }
        }
    }

    private void validateScoreAndWeight(BigDecimal maxScore, BigDecimal weight) {
        if (maxScore == null || maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Điểm tối đa phải lớn hơn 0.");
        }
        if (weight != null && weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Trọng số phải lớn hơn 0.");
        }
    }

    private AssessmentResponse toResponse(Assessment assessment) {
        return new AssessmentResponse(
                assessment.getId(),
                assessment.getClassroomId(),
                assessment.getClassSessionId(),
                assessment.getEvaluationPeriodId(),
                assessment.getTitle(),
                assessment.getDescription(),
                assessment.getType(),
                assessment.getAssessmentDate(),
                assessment.getMaxScore(),
                assessment.getWeight(),
                assessment.getStatus(),
                assessment.getPublishedToStudents(),
                assessment.getCreatedByTeacherId(),
                assessment.getCreatedBy(),
                assessment.getCreatedAt(),
                assessment.getUpdatedAt()
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
