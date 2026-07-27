package com.englishcenter.academic.report;

import com.englishcenter.academic.common.AcademicAccessService;
import com.englishcenter.academic.evaluation.EvaluationPeriod;
import com.englishcenter.academic.evaluation.EvaluationPeriodRepository;
import com.englishcenter.academic.evaluation.StudentEvaluation;
import com.englishcenter.academic.evaluation.StudentEvaluationRepository;
import com.englishcenter.academic.evaluation.StudentEvaluationStatus;
import com.englishcenter.academic.progress.AcademicProgressCalculationService;
import com.englishcenter.academic.progress.dto.AcademicProgressSummaryResponse;
import com.englishcenter.academic.progress.dto.AssessmentScoreBreakdownItem;
import com.englishcenter.academic.report.dto.ProgressReportDocumentResponse;
import com.englishcenter.academic.report.dto.ProgressReportResponse;
import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.center.CenterProfileService;
import com.englishcenter.center.dto.CenterProfileResponse;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.security.SecurityUtils;
import com.englishcenter.student.Student;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.teacher.Teacher;
import com.englishcenter.teacher.TeacherRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentProgressReportService {
    private final StudentProgressReportRepository reportRepository;
    private final StudentEvaluationRepository studentEvaluationRepository;
    private final EvaluationPeriodRepository evaluationPeriodRepository;
    private final AcademicProgressCalculationService progressCalculationService;
    private final CenterProfileService centerProfileService;
    private final ClassroomRepository classroomRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AcademicAccessService academicAccessService;

    public StudentProgressReportService(
            StudentProgressReportRepository reportRepository,
            StudentEvaluationRepository studentEvaluationRepository,
            EvaluationPeriodRepository evaluationPeriodRepository,
            AcademicProgressCalculationService progressCalculationService,
            CenterProfileService centerProfileService,
            ClassroomRepository classroomRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            AcademicAccessService academicAccessService
    ) {
        this.reportRepository = reportRepository;
        this.studentEvaluationRepository = studentEvaluationRepository;
        this.evaluationPeriodRepository = evaluationPeriodRepository;
        this.progressCalculationService = progressCalculationService;
        this.centerProfileService = centerProfileService;
        this.classroomRepository = classroomRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.academicAccessService = academicAccessService;
    }

    @Transactional
    public ProgressReportResponse createOrSyncOnPublish(StudentEvaluation evaluation) {
        return upsertFromEvaluation(evaluation, ProgressReportStatus.PUBLISHED, true);
    }

    @Transactional
    public ProgressReportResponse createOrSyncOnFinalize(StudentEvaluation evaluation) {
        return upsertFromEvaluation(evaluation, ProgressReportStatus.FINALIZED, true);
    }

    @Transactional
    public ProgressReportResponse syncStatus(StudentEvaluation evaluation) {
        ProgressReportStatus status = evaluation.getStatus() == StudentEvaluationStatus.FINALIZED
                ? ProgressReportStatus.FINALIZED
                : ProgressReportStatus.PUBLISHED;
        return upsertFromEvaluation(evaluation, status, false);
    }

    @Transactional(readOnly = true)
    public Page<ProgressReportResponse> search(
            Long classroomId,
            Long studentId,
            Long periodId,
            ProgressReportStatus status,
            int page,
            int size
    ) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.STUDENT) {
            throw new BusinessException("Học viên vui lòng dùng API /api/me/progress-reports.");
        }
        if (classroomId != null) {
            academicAccessService.requireAccessClassroom(classroomId);
        }
        if (studentId != null) {
            academicAccessService.requireSelfOrManagerOfStudent(studentId);
        }
        return reportRepository
                .search(classroomId, studentId, periodId, status, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProgressReportDocumentResponse getDocument(Long id) {
        StudentProgressReport report = require(id);
        requireAccess(report);
        return buildDocument(report);
    }

    @Transactional(readOnly = true)
    public List<ProgressReportResponse> listForCurrentStudent() {
        Long studentId = SecurityUtils.requireLinkedStudentId();
        return reportRepository.findByStudentIdOrderByGeneratedAtDescIdDesc(studentId).stream()
                .filter(report -> report.getStatus() == ProgressReportStatus.PUBLISHED
                        || report.getStatus() == ProgressReportStatus.FINALIZED)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgressReportDocumentResponse getDocumentForCurrentStudent(Long id) {
        Long studentId = SecurityUtils.requireLinkedStudentId();
        StudentProgressReport report = reportRepository.findByIdAndStudentId(id, studentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu tổng kết."));
        if (report.getStatus() != ProgressReportStatus.PUBLISHED
                && report.getStatus() != ProgressReportStatus.FINALIZED) {
            throw new NotFoundException("Không tìm thấy phiếu tổng kết.");
        }
        return buildDocument(report);
    }

    private ProgressReportResponse upsertFromEvaluation(
            StudentEvaluation evaluation,
            ProgressReportStatus status,
            boolean bumpGeneratedAt
    ) {
        StudentProgressReport report = reportRepository.findByEvaluationId(evaluation.getId())
                .orElseGet(StudentProgressReport::new);
        report.setEvaluationId(evaluation.getId());
        report.setEvaluationPeriodId(evaluation.getEvaluationPeriodId());
        report.setClassroomId(evaluation.getClassroomId());
        report.setStudentId(evaluation.getStudentId());
        report.setTeacherId(evaluation.getTeacherId());
        report.setStatus(status);
        report.setPublishedAt(evaluation.getPublishedAt());
        if (bumpGeneratedAt || report.getGeneratedAt() == null) {
            report.setGeneratedAt(LocalDateTime.now());
        }
        return toResponse(reportRepository.save(report));
    }

    private ProgressReportDocumentResponse buildDocument(StudentProgressReport report) {
        StudentEvaluation evaluation = studentEvaluationRepository.findById(report.getEvaluationId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhận xét gắn với phiếu tổng kết."));
        EvaluationPeriod period = evaluationPeriodRepository.findById(report.getEvaluationPeriodId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ đánh giá."));
        Student student = studentRepository.findById(report.getStudentId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy học viên."));
        Classroom classroom = classroomRepository.findById(report.getClassroomId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học."));

        String teacherName = classroom.getTeacherName();
        Long teacherId = report.getTeacherId() != null ? report.getTeacherId() : classroom.getTeacherId();
        if (teacherId != null) {
            teacherName = teacherRepository.findById(teacherId)
                    .map(Teacher::getFullName)
                    .orElse(teacherName);
        }

        AcademicProgressSummaryResponse summary = progressCalculationService.calculate(
                report.getStudentId(),
                report.getClassroomId(),
                report.getEvaluationPeriodId()
        );
        List<AssessmentScoreBreakdownItem> breakdown = progressCalculationService.assessmentBreakdown(
                report.getStudentId(),
                report.getClassroomId(),
                report.getEvaluationPeriodId()
        );
        CenterProfileResponse centerProfile;
        try {
            centerProfile = centerProfileService.getProfile();
        } catch (NotFoundException ex) {
            centerProfile = null;
        }

        return new ProgressReportDocumentResponse(
                report.getId(),
                report.getStatus(),
                report.getGeneratedAt(),
                report.getPublishedAt(),
                centerProfile,
                student.getId(),
                student.getStudentCode(),
                student.getFullName(),
                classroom.getId(),
                classroom.getClassCode(),
                classroom.getClassName(),
                teacherId,
                teacherName,
                period.getId(),
                period.getName(),
                period.getStartDate(),
                period.getEndDate(),
                summary,
                breakdown,
                evaluation.getId(),
                evaluation.getStatus(),
                evaluation.getStrengths(),
                evaluation.getAreasForImprovement(),
                evaluation.getLearningAttitude(),
                evaluation.getParticipation(),
                evaluation.getHomeworkPerformance(),
                evaluation.getTeacherComment(),
                evaluation.getRecommendation(),
                evaluation.getOverallRating()
        );
    }

    private void requireAccess(StudentProgressReport report) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.ADMIN) {
            return;
        }
        if (principal.role() == AccountRole.STUDENT) {
            if (principal.studentId() == null || !principal.studentId().equals(report.getStudentId())) {
                throw new NotFoundException("Không tìm thấy phiếu tổng kết.");
            }
            return;
        }
        academicAccessService.requireAccessClassroom(report.getClassroomId());
    }

    private StudentProgressReport require(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu tổng kết."));
    }

    private ProgressReportResponse toResponse(StudentProgressReport report) {
        return new ProgressReportResponse(
                report.getId(),
                report.getEvaluationId(),
                report.getEvaluationPeriodId(),
                report.getClassroomId(),
                report.getStudentId(),
                report.getTeacherId(),
                report.getStatus(),
                report.getPublishedAt(),
                report.getGeneratedAt(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
