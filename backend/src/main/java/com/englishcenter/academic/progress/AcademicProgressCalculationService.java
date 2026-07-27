package com.englishcenter.academic.progress;

import com.englishcenter.academic.assessment.Assessment;
import com.englishcenter.academic.assessment.AssessmentRepository;
import com.englishcenter.academic.assessment.AssessmentStatus;
import com.englishcenter.academic.assignment.Assignment;
import com.englishcenter.academic.assignment.AssignmentRepository;
import com.englishcenter.academic.assignment.AssignmentStatus;
import com.englishcenter.academic.assignment.AssignmentTargetMode;
import com.englishcenter.academic.assignment.AssignmentTargetRepository;
import com.englishcenter.academic.evaluation.EvaluationPeriod;
import com.englishcenter.academic.evaluation.EvaluationPeriodRepository;
import com.englishcenter.academic.progress.dto.AcademicProgressSummaryResponse;
import com.englishcenter.academic.progress.dto.AssessmentScoreBreakdownItem;
import com.englishcenter.academic.score.AssessmentScore;
import com.englishcenter.academic.score.AssessmentScoreRepository;
import com.englishcenter.academic.score.AssessmentScoreStatus;
import com.englishcenter.academic.submission.AssignmentSubmission;
import com.englishcenter.academic.submission.AssignmentSubmissionRepository;
import com.englishcenter.academic.submission.SubmissionStatus;
import com.englishcenter.attendance.Attendance;
import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.attendance.AttendanceStatus;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicProgressCalculationService {
    private static final int INTERNAL_SCALE = 4;
    private static final int DISPLAY_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private static final Set<AssignmentStatus> ASSIGNMENT_STATUSES =
            EnumSet.of(AssignmentStatus.PUBLISHED, AssignmentStatus.CLOSED);
    private static final Set<SubmissionStatus> SUBMITTED_STATUSES = EnumSet.of(
            SubmissionStatus.SUBMITTED,
            SubmissionStatus.LATE,
            SubmissionStatus.GRADED,
            SubmissionStatus.RETURNED
    );

    private final AttendanceRepository attendanceRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentTargetRepository assignmentTargetRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentScoreRepository assessmentScoreRepository;
    private final EvaluationPeriodRepository evaluationPeriodRepository;
    private final EnrollmentRepository enrollmentRepository;

    public AcademicProgressCalculationService(
            AttendanceRepository attendanceRepository,
            AssignmentRepository assignmentRepository,
            AssignmentTargetRepository assignmentTargetRepository,
            AssignmentSubmissionRepository assignmentSubmissionRepository,
            AssessmentRepository assessmentRepository,
            AssessmentScoreRepository assessmentScoreRepository,
            EvaluationPeriodRepository evaluationPeriodRepository,
            EnrollmentRepository enrollmentRepository
    ) {
        this.attendanceRepository = attendanceRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentTargetRepository = assignmentTargetRepository;
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentScoreRepository = assessmentScoreRepository;
        this.evaluationPeriodRepository = evaluationPeriodRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public AcademicProgressSummaryResponse calculate(Long studentId, Long classroomId, Long evaluationPeriodId) {
        EvaluationPeriod evaluationPeriod = resolveEvaluationPeriod(evaluationPeriodId);

        AttendanceCounts attendanceCounts = countAttendance(studentId, classroomId, evaluationPeriod);
        AssignmentCounts assignmentCounts = countAssignments(studentId, classroomId, evaluationPeriod);
        List<Assessment> assessments = loadAssessments(classroomId, evaluationPeriod);
        Map<Long, AssessmentScore> scoresByAssessmentId = loadScoresByAssessmentId(studentId, assessments);
        AssessmentAverages assessmentAverages = computeAssessmentAverages(assessments, scoresByAssessmentId);
        EnrollmentSnapshot enrollmentSnapshot = loadEnrollmentSnapshot(studentId, classroomId);

        return new AcademicProgressSummaryResponse(
                studentId,
                classroomId,
                evaluationPeriodId,
                attendanceCounts.sessionsHeld(),
                attendanceCounts.sessionsPresent(),
                attendanceCounts.sessionsAbsent(),
                attendanceCounts.sessionsExcused(),
                attendanceCounts.attendanceRate(),
                assignmentCounts.assigned(),
                assignmentCounts.submitted(),
                assignmentCounts.late(),
                assignmentCounts.graded(),
                assignmentCounts.completionRate(),
                assessments.size(),
                assessmentAverages.averagePercentage(),
                assessmentAverages.weightedAverage(),
                enrollmentSnapshot.totalSessions(),
                enrollmentSnapshot.usedSessions(),
                enrollmentSnapshot.remainingSessions()
        );
    }

    @Transactional(readOnly = true)
    public List<AssessmentScoreBreakdownItem> assessmentBreakdown(
            Long studentId,
            Long classroomId,
            Long evaluationPeriodId
    ) {
        EvaluationPeriod evaluationPeriod = resolveEvaluationPeriod(evaluationPeriodId);
        List<Assessment> assessments = loadAssessments(classroomId, evaluationPeriod);
        Map<Long, AssessmentScore> scoresByAssessmentId = loadScoresByAssessmentId(studentId, assessments);

        return assessments.stream()
                .map(assessment -> toBreakdownItem(assessment, scoresByAssessmentId.get(assessment.getId())))
                .toList();
    }

    public static BigDecimal normalizePercentage(BigDecimal score, BigDecimal maxScore) {
        if (score == null || maxScore == null || maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return score.multiply(HUNDRED)
                .divide(maxScore, INTERNAL_SCALE, RoundingMode.HALF_UP)
                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    public static boolean isIncludedInAssessmentAverage(AssessmentScore score) {
        if (score == null || score.getScore() == null) {
            return false;
        }
        // EXEMPT never contributes. ABSENT only contributes when teacher explicitly entered a score.
        if (score.getStatus() == AssessmentScoreStatus.EXEMPT
                || score.getStatus() == AssessmentScoreStatus.NOT_GRADED) {
            return false;
        }
        return score.getStatus() == AssessmentScoreStatus.GRADED
                || score.getStatus() == AssessmentScoreStatus.ABSENT;
    }

    public static BigDecimal computeUnweightedAverage(List<BigDecimal> normalizedPercentages) {
        if (normalizedPercentages == null || normalizedPercentages.isEmpty()) {
            return null;
        }
        List<BigDecimal> included = normalizedPercentages.stream()
                .filter(Objects::nonNull)
                .toList();
        if (included.isEmpty()) {
            return null;
        }
        BigDecimal sum = included.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(included.size()), INTERNAL_SCALE, RoundingMode.HALF_UP)
                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal computeWeightedAverage(
            List<BigDecimal> normalizedPercentages,
            List<BigDecimal> weights
    ) {
        if (normalizedPercentages == null
                || weights == null
                || normalizedPercentages.isEmpty()
                || normalizedPercentages.size() != weights.size()) {
            return null;
        }

        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (int index = 0; index < normalizedPercentages.size(); index++) {
            BigDecimal normalized = normalizedPercentages.get(index);
            BigDecimal weight = weights.get(index);
            if (normalized == null || weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            weightedSum = weightedSum.add(normalized.multiply(weight));
            totalWeight = totalWeight.add(weight);
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        return weightedSum.divide(totalWeight, INTERNAL_SCALE, RoundingMode.HALF_UP)
                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    static BigDecimal computeAttendanceRate(int sessionsPresent, int sessionsHeld) {
        if (sessionsHeld <= 0) {
            return null;
        }
        return BigDecimal.valueOf(sessionsPresent)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(sessionsHeld), INTERNAL_SCALE, RoundingMode.HALF_UP)
                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    static BigDecimal computeCompletionRate(int submitted, int assigned) {
        if (assigned <= 0) {
            return null;
        }
        return BigDecimal.valueOf(submitted)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(assigned), INTERNAL_SCALE, RoundingMode.HALF_UP)
                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    private EvaluationPeriod resolveEvaluationPeriod(Long evaluationPeriodId) {
        if (evaluationPeriodId == null) {
            return null;
        }
        return evaluationPeriodRepository.findById(evaluationPeriodId)
                .orElseThrow(() -> new NotFoundException("Evaluation period not found"));
    }

    private AttendanceCounts countAttendance(
            Long studentId,
            Long classroomId,
            EvaluationPeriod evaluationPeriod
    ) {
        List<Attendance> attendances = attendanceRepository.findValidByStudentIdAndClassroomId(studentId, classroomId)
                .stream()
                .filter(attendance -> isWithinPeriod(attendance.getSession(), evaluationPeriod))
                .toList();

        int sessionsPresent = 0;
        int sessionsAbsent = 0;
        int sessionsExcused = 0;
        for (Attendance attendance : attendances) {
            switch (attendance.getStatus()) {
                case PRESENT -> sessionsPresent++;
                case ABSENT -> sessionsAbsent++;
                case EXCUSED -> sessionsExcused++;
            }
        }

        int sessionsHeld = sessionsPresent + sessionsAbsent + sessionsExcused;
        return new AttendanceCounts(
                sessionsHeld,
                sessionsPresent,
                sessionsAbsent,
                sessionsExcused,
                computeAttendanceRate(sessionsPresent, sessionsHeld)
        );
    }

    private AssignmentCounts countAssignments(
            Long studentId,
            Long classroomId,
            EvaluationPeriod evaluationPeriod
    ) {
        List<Assignment> assignments = assignmentRepository.findByClassroomIdAndStatusIn(
                        classroomId,
                        ASSIGNMENT_STATUSES
                )
                .stream()
                .filter(assignment -> isWithinPeriod(assignment.getAssignedDate(), evaluationPeriod))
                .filter(assignment -> isStudentTargeted(assignment, studentId))
                .toList();

        if (assignments.isEmpty()) {
            return new AssignmentCounts(0, 0, 0, 0, null);
        }

        List<Long> assignmentIds = assignments.stream().map(Assignment::getId).toList();
        Map<Long, AssignmentSubmission> submissionsByAssignmentId = new HashMap<>();
        for (AssignmentSubmission submission : assignmentSubmissionRepository.findByStudentIdAndAssignmentIdIn(
                studentId,
                assignmentIds
        )) {
            submissionsByAssignmentId.put(submission.getAssignmentId(), submission);
        }

        int submitted = 0;
        int late = 0;
        int graded = 0;
        for (Assignment assignment : assignments) {
            AssignmentSubmission submission = submissionsByAssignmentId.get(assignment.getId());
            if (submission == null) {
                continue;
            }
            if (SUBMITTED_STATUSES.contains(submission.getStatus())) {
                submitted++;
            }
            if (submission.getStatus() == SubmissionStatus.LATE) {
                late++;
            }
            if (isGradedSubmission(submission)) {
                graded++;
            }
        }

        return new AssignmentCounts(
                assignments.size(),
                submitted,
                late,
                graded,
                computeCompletionRate(submitted, assignments.size())
        );
    }

    private List<Assessment> loadAssessments(Long classroomId, EvaluationPeriod evaluationPeriod) {
        return assessmentRepository.findByClassroomIdAndStatusNot(classroomId, AssessmentStatus.CANCELED)
                .stream()
                .filter(assessment -> matchesAssessmentPeriod(assessment, evaluationPeriod))
                .toList();
    }

    private Map<Long, AssessmentScore> loadScoresByAssessmentId(Long studentId, List<Assessment> assessments) {
        if (assessments.isEmpty()) {
            return Map.of();
        }
        List<Long> assessmentIds = assessments.stream().map(Assessment::getId).toList();
        Map<Long, AssessmentScore> scoresByAssessmentId = new HashMap<>();
        for (AssessmentScore score : assessmentScoreRepository.findByStudentIdAndAssessmentIdIn(
                studentId,
                assessmentIds
        )) {
            scoresByAssessmentId.put(score.getAssessmentId(), score);
        }
        return scoresByAssessmentId;
    }

    private AssessmentAverages computeAssessmentAverages(
            List<Assessment> assessments,
            Map<Long, AssessmentScore> scoresByAssessmentId
    ) {
        List<BigDecimal> includedPercentages = assessments.stream()
                .map(assessment -> scoresByAssessmentId.get(assessment.getId()))
                .filter(AcademicProgressCalculationService::isIncludedInAssessmentAverage)
                .map(score -> normalizePercentage(score.getScore(), findAssessmentMaxScore(assessments, score)))
                .filter(Objects::nonNull)
                .toList();

        List<BigDecimal> weightedPercentages = new java.util.ArrayList<>();
        List<BigDecimal> weights = new java.util.ArrayList<>();
        for (Assessment assessment : assessments) {
            AssessmentScore score = scoresByAssessmentId.get(assessment.getId());
            if (!isIncludedInAssessmentAverage(score)) {
                continue;
            }
            BigDecimal normalized = normalizePercentage(score.getScore(), assessment.getMaxScore());
            if (normalized == null) {
                continue;
            }
            weightedPercentages.add(normalized);
            weights.add(assessment.getWeight());
        }

        return new AssessmentAverages(
                computeUnweightedAverage(includedPercentages),
                computeWeightedAverage(weightedPercentages, weights)
        );
    }

    private BigDecimal findAssessmentMaxScore(List<Assessment> assessments, AssessmentScore score) {
        return assessments.stream()
                .filter(assessment -> assessment.getId().equals(score.getAssessmentId()))
                .map(Assessment::getMaxScore)
                .findFirst()
                .orElse(null);
    }

    private AssessmentScoreBreakdownItem toBreakdownItem(Assessment assessment, AssessmentScore score) {
        BigDecimal rawScore = score == null ? null : score.getScore();
        AssessmentScoreStatus status = score == null ? AssessmentScoreStatus.NOT_GRADED : score.getStatus();
        BigDecimal normalizedPercentage = normalizePercentage(rawScore, assessment.getMaxScore());

        return new AssessmentScoreBreakdownItem(
                assessment.getId(),
                assessment.getTitle(),
                assessment.getAssessmentDate(),
                assessment.getType(),
                assessment.getMaxScore(),
                rawScore,
                status,
                normalizedPercentage,
                assessment.getWeight()
        );
    }

    private EnrollmentSnapshot loadEnrollmentSnapshot(Long studentId, Long classroomId) {
        return enrollmentRepository.findFirstByStudentIdAndClassroomIdAndStatusInOrderByIdDesc(
                        studentId,
                        classroomId,
                        List.of(EnrollmentStatus.ACTIVE)
                )
                .map(this::toEnrollmentSnapshot)
                .orElse(new EnrollmentSnapshot(null, null, null));
    }

    private EnrollmentSnapshot toEnrollmentSnapshot(Enrollment enrollment) {
        int totalSessions = enrollment.getTotalSessions();
        int usedSessions = enrollment.getUsedSessions();
        return new EnrollmentSnapshot(totalSessions, usedSessions, totalSessions - usedSessions);
    }

    private boolean isStudentTargeted(Assignment assignment, Long studentId) {
        if (assignment.getTargetMode() == AssignmentTargetMode.ENTIRE_CLASS) {
            return true;
        }
        return assignmentTargetRepository.existsByAssignmentIdAndStudentId(assignment.getId(), studentId);
    }

    private boolean isGradedSubmission(AssignmentSubmission submission) {
        if (submission.getStatus() == SubmissionStatus.GRADED) {
            return true;
        }
        return submission.getStatus() == SubmissionStatus.RETURNED && submission.getTeacherScore() != null;
    }

    private boolean isWithinPeriod(ClassSession session, EvaluationPeriod evaluationPeriod) {
        return isWithinPeriod(session.getSessionDate(), evaluationPeriod);
    }

    private boolean isWithinPeriod(LocalDate date, EvaluationPeriod evaluationPeriod) {
        if (evaluationPeriod == null || date == null) {
            return true;
        }
        return !date.isBefore(evaluationPeriod.getStartDate()) && !date.isAfter(evaluationPeriod.getEndDate());
    }

    private boolean matchesAssessmentPeriod(Assessment assessment, EvaluationPeriod evaluationPeriod) {
        if (evaluationPeriod == null) {
            return true;
        }
        if (evaluationPeriod.getId().equals(assessment.getEvaluationPeriodId())) {
            return true;
        }
        return isWithinPeriod(assessment.getAssessmentDate(), evaluationPeriod);
    }

    private record AttendanceCounts(
            int sessionsHeld,
            int sessionsPresent,
            int sessionsAbsent,
            int sessionsExcused,
            BigDecimal attendanceRate
    ) {
    }

    private record AssignmentCounts(
            int assigned,
            int submitted,
            int late,
            int graded,
            BigDecimal completionRate
    ) {
    }

    private record AssessmentAverages(
            BigDecimal averagePercentage,
            BigDecimal weightedAverage
    ) {
    }

    private record EnrollmentSnapshot(
            Integer totalSessions,
            Integer usedSessions,
            Integer remainingSessions
    ) {
    }
}
