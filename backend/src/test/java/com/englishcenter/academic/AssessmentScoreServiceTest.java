package com.englishcenter.academic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishcenter.academic.assessment.Assessment;
import com.englishcenter.academic.assessment.AssessmentRepository;
import com.englishcenter.academic.assessment.AssessmentService;
import com.englishcenter.academic.assessment.AssessmentStatus;
import com.englishcenter.academic.common.AcademicAccessService;
import com.englishcenter.academic.score.AssessmentScore;
import com.englishcenter.academic.score.AssessmentScoreRepository;
import com.englishcenter.academic.score.AssessmentScoreService;
import com.englishcenter.academic.score.AssessmentScoreStatus;
import com.englishcenter.academic.score.dto.BulkScoreUpdateRequest;
import com.englishcenter.academic.score.dto.ScoreRowRequest;
import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.AccountStatus;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.student.Student;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AssessmentScoreServiceTest {
    @Mock
    private AssessmentScoreRepository assessmentScoreRepository;
    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private AssessmentService assessmentService;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private AcademicAccessService academicAccessService;

    private AssessmentScoreService service;

    @BeforeEach
    void setUp() {
        service = new AssessmentScoreService(
                assessmentScoreRepository,
                assessmentRepository,
                assessmentService,
                enrollmentRepository,
                academicAccessService
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin(), null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bulkUpdateRejectsScoreAboveMaxWithoutSaving() {
        Assessment assessment = assessment(50);
        when(assessmentService.require(1L)).thenReturn(assessment);
        when(enrollmentRepository.findByClassroomIdOrderByStartDateDescIdDesc(10L))
                .thenReturn(List.of(enrollment(1L), enrollment(2L)));

        BulkScoreUpdateRequest request = new BulkScoreUpdateRequest(List.of(
                new ScoreRowRequest(1L, new BigDecimal("40"), AssessmentScoreStatus.GRADED, null),
                new ScoreRowRequest(2L, new BigDecimal("51"), AssessmentScoreStatus.GRADED, null)
        ));

        assertThatThrownBy(() -> service.bulkUpdate(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("điểm phải từ 0 đến 50");

        verify(assessmentScoreRepository, never()).save(any());
    }

    @Test
    void bulkUpdateAcceptsAbsentAndExemptWithoutForcingZero() {
        Assessment assessment = assessment(50);
        when(assessmentService.require(1L)).thenReturn(assessment);
        when(enrollmentRepository.findByClassroomIdOrderByStartDateDescIdDesc(10L))
                .thenReturn(List.of(enrollment(1L), enrollment(2L), enrollment(3L)));
        when(assessmentScoreRepository.findByAssessmentIdAndStudentId(eq(1L), any()))
                .thenReturn(Optional.empty());
        when(assessmentScoreRepository.save(any(AssessmentScore.class))).thenAnswer(invocation -> {
            AssessmentScore score = invocation.getArgument(0);
            score.setId(score.getStudentId());
            return score;
        });

        BulkScoreUpdateRequest request = new BulkScoreUpdateRequest(List.of(
                new ScoreRowRequest(1L, new BigDecimal("40"), AssessmentScoreStatus.GRADED, "Good"),
                new ScoreRowRequest(2L, null, AssessmentScoreStatus.ABSENT, null),
                new ScoreRowRequest(3L, null, AssessmentScoreStatus.EXEMPT, null)
        ));

        var responses = service.bulkUpdate(1L, request);
        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).score()).isEqualByComparingTo("40");
        assertThat(responses.get(1).status()).isEqualTo(AssessmentScoreStatus.ABSENT);
        assertThat(responses.get(1).score()).isNull();
        assertThat(responses.get(2).status()).isEqualTo(AssessmentScoreStatus.EXEMPT);
    }

    private Assessment assessment(int maxScore) {
        Assessment assessment = new Assessment();
        assessment.setId(1L);
        assessment.setClassroomId(10L);
        assessment.setMaxScore(BigDecimal.valueOf(maxScore));
        assessment.setStatus(AssessmentStatus.OPEN);
        assessment.setPublishedToStudents(false);
        return assessment;
    }

    private Enrollment enrollment(Long studentId) {
        Student student = new Student();
        student.setId(studentId);
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        return enrollment;
    }

    private AccountPrincipal admin() {
        return new AccountPrincipal(
                1L,
                "admin",
                AccountRole.ADMIN,
                AccountStatus.ACTIVE,
                null,
                null,
                false,
                0
        );
    }
}
