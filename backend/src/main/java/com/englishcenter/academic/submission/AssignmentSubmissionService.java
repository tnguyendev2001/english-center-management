package com.englishcenter.academic.submission;

import com.englishcenter.academic.assignment.Assignment;
import com.englishcenter.academic.assignment.AssignmentRepository;
import com.englishcenter.academic.assignment.AssignmentService;
import com.englishcenter.academic.assignment.AssignmentStatus;
import com.englishcenter.academic.common.AcademicAccessService;
import com.englishcenter.academic.storage.FileStorageService;
import com.englishcenter.academic.storage.StoredFile;
import com.englishcenter.academic.submission.dto.AssignmentSubmissionAttachmentResponse;
import com.englishcenter.academic.submission.dto.AssignmentSubmissionResponse;
import com.englishcenter.academic.submission.dto.GradeSubmissionRequest;
import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AssignmentSubmissionService {
    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentSubmissionAttachmentRepository attachmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentService assignmentService;
    private final AcademicAccessService academicAccessService;
    private final FileStorageService fileStorageService;

    public AssignmentSubmissionService(
            AssignmentSubmissionRepository submissionRepository,
            AssignmentSubmissionAttachmentRepository attachmentRepository,
            AssignmentRepository assignmentRepository,
            AssignmentService assignmentService,
            AcademicAccessService academicAccessService,
            FileStorageService fileStorageService
    ) {
        this.submissionRepository = submissionRepository;
        this.attachmentRepository = attachmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentService = assignmentService;
        this.academicAccessService = academicAccessService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public AssignmentSubmissionResponse submitForCurrentStudent(
            Long assignmentId,
            String textAnswer,
            List<MultipartFile> files
    ) {
        Long studentId = SecurityUtils.requireLinkedStudentId();
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài tập."));
        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new BusinessException("Chỉ có thể nộp bài tập đang mở.");
        }
        if (!Boolean.TRUE.equals(assignment.getAllowSubmission())) {
            throw new BusinessException("Bài tập này không cho phép nộp bài trực tuyến.");
        }
        if (!assignmentService.isEligibleStudent(assignment, studentId)) {
            throw new BusinessException("Bạn không thuộc đối tượng được giao bài tập này.");
        }
        LocalDate today = assignmentService.today();
        boolean late = assignment.getDueDate() != null && today.isAfter(assignment.getDueDate());
        if (late && !Boolean.TRUE.equals(assignment.getAllowLateSubmission())) {
            throw new BusinessException("Đã quá hạn nộp bài tập.");
        }

        AssignmentSubmission existing = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElse(null);
        if (existing != null) {
            if (existing.getStatus() == SubmissionStatus.GRADED
                    || existing.getStatus() == SubmissionStatus.RETURNED) {
                throw new BusinessException("Bài nộp đã được chấm điểm, không thể nộp lại.");
            }
            if (textAnswer != null) {
                existing.setTextAnswer(trimToNull(textAnswer));
            }
            if (late) {
                existing.setStatus(SubmissionStatus.LATE);
            }
            AssignmentSubmission saved = submissionRepository.save(existing);
            addAttachments(saved.getId(), files);
            return toResponse(saved);
        }

        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setAssignmentId(assignmentId);
        submission.setStudentId(studentId);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setTextAnswer(trimToNull(textAnswer));
        submission.setStatus(late ? SubmissionStatus.LATE : SubmissionStatus.SUBMITTED);
        AssignmentSubmission saved = submissionRepository.save(submission);
        addAttachments(saved.getId(), files);
        return toResponse(saved);
    }

    @Transactional
    public AssignmentSubmissionResponse grade(Long submissionId, GradeSubmissionRequest request) {
        AssignmentSubmission submission = require(submissionId);
        Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài tập."));
        academicAccessService.requireManageClassroom(assignment.getClassroomId());

        SubmissionStatus status = request.status();
        if (status != SubmissionStatus.GRADED && status != SubmissionStatus.RETURNED) {
            throw new BusinessException("Trạng thái chấm điểm phải là GRADED hoặc RETURNED.");
        }
        if (request.teacherScore() != null) {
            if (assignment.getMaxScore() == null) {
                throw new BusinessException("Bài tập không có điểm tối đa; chỉ nhận xét định tính.");
            }
            if (request.teacherScore().compareTo(BigDecimal.ZERO) < 0
                    || request.teacherScore().compareTo(assignment.getMaxScore()) > 0) {
                throw new BusinessException("Điểm phải từ 0 đến điểm tối đa của bài tập.");
            }
        } else if (assignment.getMaxScore() != null && status == SubmissionStatus.GRADED) {
            throw new BusinessException("Vui lòng nhập điểm khi chấm bài.");
        }

        submission.setTeacherScore(request.teacherScore());
        submission.setTeacherFeedback(trimToNull(request.teacherFeedback()));
        submission.setStatus(status);
        submission.setGradedAt(LocalDateTime.now());
        submission.setGradedBy(SecurityUtils.currentUsernameOrSystem());
        return toResponse(submissionRepository.save(submission));
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> listByAssignment(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài tập."));
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.STUDENT) {
            throw new BusinessException("Học viên không thể xem danh sách bài nộp của lớp.");
        }
        academicAccessService.requireAccessClassroom(assignment.getClassroomId());
        return submissionRepository.findByAssignmentIdOrderBySubmittedAtDescIdDesc(assignmentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> listForCurrentStudent() {
        Long studentId = SecurityUtils.requireLinkedStudentId();
        return submissionRepository.findByStudentIdOrderBySubmittedAtDescIdDesc(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void addAttachments(Long submissionId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            StoredFile stored = fileStorageService.store(file, "submissions");
            AssignmentSubmissionAttachment attachment = new AssignmentSubmissionAttachment();
            attachment.setSubmissionId(submissionId);
            attachment.setFileName(stored.originalFileName());
            attachment.setStoredFileName(stored.storedFileName());
            attachment.setContentType(stored.contentType());
            attachment.setFileSize(stored.fileSize());
            attachment.setStorageKey(stored.storageKey());
            attachmentRepository.save(attachment);
        }
    }

    private AssignmentSubmission require(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài nộp."));
    }

    private AssignmentSubmissionResponse toResponse(AssignmentSubmission submission) {
        List<AssignmentSubmissionAttachmentResponse> attachments = attachmentRepository
                .findBySubmissionIdOrderByUploadedAtAscIdAsc(submission.getId())
                .stream()
                .map(attachment -> new AssignmentSubmissionAttachmentResponse(
                        attachment.getId(),
                        attachment.getFileName(),
                        attachment.getContentType(),
                        attachment.getFileSize(),
                        attachment.getUploadedAt()
                ))
                .toList();
        return new AssignmentSubmissionResponse(
                submission.getId(),
                submission.getAssignmentId(),
                submission.getStudentId(),
                submission.getSubmittedAt(),
                submission.getTextAnswer(),
                submission.getStatus(),
                submission.getTeacherScore(),
                submission.getTeacherFeedback(),
                submission.getGradedAt(),
                submission.getGradedBy(),
                attachments,
                submission.getCreatedAt(),
                submission.getUpdatedAt()
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
