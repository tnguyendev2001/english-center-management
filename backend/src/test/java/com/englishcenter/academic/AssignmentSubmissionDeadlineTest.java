package com.englishcenter.academic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.englishcenter.academic.assignment.Assignment;
import com.englishcenter.academic.assignment.AssignmentRepository;
import com.englishcenter.academic.assignment.AssignmentService;
import com.englishcenter.academic.assignment.AssignmentStatus;
import com.englishcenter.academic.common.AcademicAccessService;
import com.englishcenter.academic.storage.FileStorageService;
import com.englishcenter.academic.submission.AssignmentSubmissionRepository;
import com.englishcenter.academic.submission.AssignmentSubmissionAttachmentRepository;
import com.englishcenter.academic.submission.AssignmentSubmissionService;
import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.AccountStatus;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.common.exception.BusinessException;
import java.time.LocalDate;
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
class AssignmentSubmissionDeadlineTest {
    @Mock
    private AssignmentSubmissionRepository submissionRepository;
    @Mock
    private AssignmentSubmissionAttachmentRepository attachmentRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private AssignmentService assignmentService;
    @Mock
    private AcademicAccessService academicAccessService;
    @Mock
    private FileStorageService fileStorageService;

    private AssignmentSubmissionService service;

    @BeforeEach
    void setUp() {
        service = new AssignmentSubmissionService(
                submissionRepository,
                attachmentRepository,
                assignmentRepository,
                assignmentService,
                academicAccessService,
                fileStorageService
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(student(7L), null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsLateSubmissionWhenLateNotAllowed() {
        Assignment assignment = publishedAssignment(false);
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(assignmentService.isEligibleStudent(assignment, 7L)).thenReturn(true);
        when(assignmentService.today()).thenReturn(LocalDate.of(2026, 8, 10));

        assertThatThrownBy(() -> service.submitForCurrentStudent(1L, "answer", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("quá hạn");
    }

    @Test
    void marksLateWhenLateAllowed() {
        Assignment assignment = publishedAssignment(true);
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(assignmentService.isEligibleStudent(assignment, 7L)).thenReturn(true);
        when(assignmentService.today()).thenReturn(LocalDate.of(2026, 8, 10));
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 7L)).thenReturn(Optional.empty());
        when(submissionRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            var submission = invocation.getArgument(0, com.englishcenter.academic.submission.AssignmentSubmission.class);
            submission.setId(99L);
            return submission;
        });
        when(attachmentRepository.findBySubmissionIdOrderByUploadedAtAscIdAsc(99L)).thenReturn(List.of());

        var response = service.submitForCurrentStudent(1L, "late answer", null);
        assertThat(response.status().name()).isEqualTo("LATE");
    }

    private Assignment publishedAssignment(boolean allowLate) {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setClassroomId(10L);
        assignment.setStatus(AssignmentStatus.PUBLISHED);
        assignment.setAllowSubmission(true);
        assignment.setAllowLateSubmission(allowLate);
        assignment.setDueDate(LocalDate.of(2026, 8, 1));
        return assignment;
    }

    private AccountPrincipal student(Long studentId) {
        return new AccountPrincipal(
                2L,
                "student",
                AccountRole.STUDENT,
                AccountStatus.ACTIVE,
                studentId,
                null,
                false,
                0
        );
    }
}
