package com.englishcenter.academic.submission;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentSubmissionAttachmentRepository extends JpaRepository<AssignmentSubmissionAttachment, Long> {
    List<AssignmentSubmissionAttachment> findBySubmissionIdOrderByUploadedAtAscIdAsc(Long submissionId);

    /**
     * Hard-deletes attachment rows. Prefer keeping submission history intact at the service layer
     * and only use this when replacing attachments during an active edit flow.
     */
    void deleteBySubmissionId(Long submissionId);
}
