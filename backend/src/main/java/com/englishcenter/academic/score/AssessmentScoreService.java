package com.englishcenter.academic.score;

import com.englishcenter.academic.assessment.Assessment;
import com.englishcenter.academic.assessment.AssessmentRepository;
import com.englishcenter.academic.assessment.AssessmentService;
import com.englishcenter.academic.assessment.AssessmentStatus;
import com.englishcenter.academic.common.AcademicAccessService;
import com.englishcenter.academic.score.dto.AssessmentScoreResponse;
import com.englishcenter.academic.score.dto.BulkScoreUpdateRequest;
import com.englishcenter.academic.score.dto.ScoreRowRequest;
import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentScoreService {
    private final AssessmentScoreRepository assessmentScoreRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentService assessmentService;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicAccessService academicAccessService;

    public AssessmentScoreService(
            AssessmentScoreRepository assessmentScoreRepository,
            AssessmentRepository assessmentRepository,
            AssessmentService assessmentService,
            EnrollmentRepository enrollmentRepository,
            AcademicAccessService academicAccessService
    ) {
        this.assessmentScoreRepository = assessmentScoreRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentService = assessmentService;
        this.enrollmentRepository = enrollmentRepository;
        this.academicAccessService = academicAccessService;
    }

    @Transactional
    public List<AssessmentScoreResponse> bulkUpdate(Long assessmentId, BulkScoreUpdateRequest request) {
        Assessment assessment = assessmentService.require(assessmentId);
        academicAccessService.requireManageClassroom(assessment.getClassroomId());
        if (assessment.getStatus() == AssessmentStatus.CANCELED) {
            throw new BusinessException("Không thể nhập điểm cho bài kiểm tra đã hủy.");
        }

        Set<Long> enrolledStudentIds = enrollmentRepository
                .findByClassroomIdOrderByStartDateDescIdDesc(assessment.getClassroomId())
                .stream()
                .map(enrollment -> enrollment.getStudent().getId())
                .collect(Collectors.toSet());

        Set<Long> seenStudents = new HashSet<>();
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < request.rows().size(); i++) {
            ScoreRowRequest row = request.rows().get(i);
            String prefix = "Dòng " + (i + 1) + ": ";
            if (!seenStudents.add(row.studentId())) {
                errors.add(prefix + "trùng học viên trong lô nhập điểm.");
                continue;
            }
            if (!enrolledStudentIds.contains(row.studentId())) {
                errors.add(prefix + "học viên không thuộc lớp học.");
                continue;
            }
            validateRow(assessment, row, errors, prefix);
        }
        if (!errors.isEmpty()) {
            throw new BusinessException(String.join(" ", errors));
        }

        String gradedBy = SecurityUtils.currentUsernameOrSystem();
        LocalDateTime now = LocalDateTime.now();
        List<AssessmentScore> saved = new ArrayList<>();
        for (ScoreRowRequest row : request.rows()) {
            AssessmentScore score = assessmentScoreRepository
                    .findByAssessmentIdAndStudentId(assessmentId, row.studentId())
                    .orElseGet(AssessmentScore::new);
            score.setAssessmentId(assessmentId);
            score.setStudentId(row.studentId());
            score.setStatus(row.status());
            score.setTeacherComment(trimToNull(row.teacherComment()));
            if (row.status() == AssessmentScoreStatus.GRADED) {
                score.setScore(row.score());
            } else if (row.status() == AssessmentScoreStatus.ABSENT || row.status() == AssessmentScoreStatus.EXEMPT) {
                score.setScore(row.score());
            } else {
                score.setScore(null);
            }
            score.setGradedBy(gradedBy);
            score.setGradedAt(now);
            saved.add(assessmentScoreRepository.save(score));
        }
        return saved.stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<AssessmentScoreResponse> publishScores(Long assessmentId) {
        Assessment assessment = assessmentService.require(assessmentId);
        academicAccessService.requireManageClassroom(assessment.getClassroomId());
        if (assessment.getStatus() == AssessmentStatus.CANCELED) {
            throw new BusinessException("Không thể công bố điểm bài kiểm tra đã hủy.");
        }
        assessment.setPublishedToStudents(true);
        assessmentRepository.save(assessment);
        LocalDateTime now = LocalDateTime.now();
        List<AssessmentScore> scores = assessmentScoreRepository.findByAssessmentIdOrderByStudentIdAsc(assessmentId);
        for (AssessmentScore score : scores) {
            if (score.getPublishedAt() == null) {
                score.setPublishedAt(now);
            }
            assessmentScoreRepository.save(score);
        }
        return scores.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AssessmentScoreResponse> listByAssessment(Long assessmentId) {
        Assessment assessment = assessmentService.require(assessmentId);
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.STUDENT) {
            throw new BusinessException("Học viên vui lòng dùng API /api/me/scores.");
        }
        academicAccessService.requireAccessClassroom(assessment.getClassroomId());
        return assessmentScoreRepository.findByAssessmentIdOrderByStudentIdAsc(assessmentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssessmentScoreResponse> listForCurrentStudent() {
        Long studentId = SecurityUtils.requireLinkedStudentId();
        Map<Long, AssessmentScore> unique = new LinkedHashMap<>();
        for (AssessmentScore score : assessmentScoreRepository.findByStudentIdOrderByCreatedAtDescIdDesc(studentId)) {
            Assessment assessment = assessmentService.require(score.getAssessmentId());
            if (!Boolean.TRUE.equals(assessment.getPublishedToStudents())) {
                continue;
            }
            if (score.getPublishedAt() == null) {
                continue;
            }
            unique.putIfAbsent(score.getId(), score);
        }
        return unique.values().stream().map(this::toResponse).toList();
    }

    private void validateRow(Assessment assessment, ScoreRowRequest row, List<String> errors, String prefix) {
        AssessmentScoreStatus status = row.status();
        if (status == AssessmentScoreStatus.GRADED) {
            if (row.score() == null) {
                errors.add(prefix + "trạng thái GRADED bắt buộc có điểm.");
                return;
            }
            if (row.score().compareTo(BigDecimal.ZERO) < 0
                    || row.score().compareTo(assessment.getMaxScore()) > 0) {
                errors.add(prefix + "điểm phải từ 0 đến " + assessment.getMaxScore() + ".");
            }
            return;
        }
        if (status == AssessmentScoreStatus.ABSENT || status == AssessmentScoreStatus.EXEMPT) {
            if (row.score() != null
                    && (row.score().compareTo(BigDecimal.ZERO) < 0
                    || row.score().compareTo(assessment.getMaxScore()) > 0)) {
                errors.add(prefix + "điểm tùy chọn phải từ 0 đến điểm tối đa.");
            }
            return;
        }
        if (status == AssessmentScoreStatus.NOT_GRADED && row.score() != null) {
            errors.add(prefix + "trạng thái NOT_GRADED không được có điểm.");
        }
    }

    private AssessmentScoreResponse toResponse(AssessmentScore score) {
        return new AssessmentScoreResponse(
                score.getId(),
                score.getAssessmentId(),
                score.getStudentId(),
                score.getScore(),
                score.getStatus(),
                score.getTeacherComment(),
                score.getGradedBy(),
                score.getGradedAt(),
                score.getPublishedAt(),
                score.getCreatedAt(),
                score.getUpdatedAt()
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
