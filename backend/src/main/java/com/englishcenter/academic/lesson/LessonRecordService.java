package com.englishcenter.academic.lesson;

import com.englishcenter.academic.common.AcademicAccessService;
import com.englishcenter.academic.lesson.dto.CreateLessonRecordRequest;
import com.englishcenter.academic.lesson.dto.LessonRecordResponse;
import com.englishcenter.academic.lesson.dto.UpdateLessonRecordRequest;
import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.security.SecurityUtils;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LessonRecordService {
    private final LessonRecordRepository lessonRecordRepository;
    private final ClassSessionRepository classSessionRepository;
    private final AcademicAccessService academicAccessService;

    public LessonRecordService(
            LessonRecordRepository lessonRecordRepository,
            ClassSessionRepository classSessionRepository,
            AcademicAccessService academicAccessService
    ) {
        this.lessonRecordRepository = lessonRecordRepository;
        this.classSessionRepository = classSessionRepository;
        this.academicAccessService = academicAccessService;
    }

    @Transactional
    public LessonRecordResponse create(CreateLessonRecordRequest request) {
        academicAccessService.requireManageSession(request.classSessionId());
        if (!classSessionRepository.existsById(request.classSessionId())) {
            throw new NotFoundException("Không tìm thấy buổi học.");
        }
        if (lessonRecordRepository.existsByClassSessionId(request.classSessionId())) {
            throw new BusinessException("Buổi học này đã có nội dung bài học.");
        }
        LessonRecord lesson = new LessonRecord();
        lesson.setClassSessionId(request.classSessionId());
        lesson.setTitle(request.title().trim());
        lesson.setObjectives(trimToNull(request.objectives()));
        lesson.setPlannedContent(trimToNull(request.plannedContent()));
        lesson.setVocabulary(trimToNull(request.vocabulary()));
        lesson.setGrammarTopics(trimToNull(request.grammarTopics()));
        lesson.setSkills(trimToNull(request.skills()));
        lesson.setHomeworkInstruction(trimToNull(request.homeworkInstruction()));
        lesson.setTeacherNote(trimToNull(request.teacherNote()));
        lesson.setStatus(LessonStatus.DRAFT);
        lesson.setCreatedBy(SecurityUtils.currentUsernameOrSystem());
        lesson.setUpdatedBy(lesson.getCreatedBy());
        return toResponse(lessonRecordRepository.save(lesson));
    }

    @Transactional
    public LessonRecordResponse update(Long id, UpdateLessonRecordRequest request) {
        LessonRecord lesson = require(id);
        academicAccessService.requireManageSession(lesson.getClassSessionId());
        lesson.setTitle(request.title().trim());
        lesson.setObjectives(trimToNull(request.objectives()));
        lesson.setPlannedContent(trimToNull(request.plannedContent()));
        lesson.setActualContent(trimToNull(request.actualContent()));
        lesson.setVocabulary(trimToNull(request.vocabulary()));
        lesson.setGrammarTopics(trimToNull(request.grammarTopics()));
        lesson.setSkills(trimToNull(request.skills()));
        lesson.setHomeworkInstruction(trimToNull(request.homeworkInstruction()));
        lesson.setTeacherNote(trimToNull(request.teacherNote()));
        lesson.setUpdatedBy(SecurityUtils.currentUsernameOrSystem());
        return toResponse(lessonRecordRepository.save(lesson));
    }

    @Transactional
    public LessonRecordResponse publish(Long id) {
        LessonRecord lesson = require(id);
        academicAccessService.requireManageSession(lesson.getClassSessionId());
        if (lesson.getStatus() != LessonStatus.DRAFT) {
            throw new BusinessException("Chỉ có thể xuất bản bài học ở trạng thái nháp.");
        }
        lesson.setStatus(LessonStatus.PUBLISHED);
        lesson.setUpdatedBy(SecurityUtils.currentUsernameOrSystem());
        return toResponse(lessonRecordRepository.save(lesson));
    }

    @Transactional
    public LessonRecordResponse complete(Long id) {
        LessonRecord lesson = require(id);
        academicAccessService.requireManageSession(lesson.getClassSessionId());
        if (lesson.getStatus() != LessonStatus.DRAFT && lesson.getStatus() != LessonStatus.PUBLISHED) {
            throw new BusinessException("Không thể hoàn thành bài học ở trạng thái hiện tại.");
        }
        lesson.setStatus(LessonStatus.COMPLETED);
        lesson.setUpdatedBy(SecurityUtils.currentUsernameOrSystem());
        return toResponse(lessonRecordRepository.save(lesson));
    }

    @Transactional(readOnly = true)
    public LessonRecordResponse getById(Long id) {
        LessonRecord lesson = require(id);
        Long classroomId = academicAccessService.classroomIdOfSession(lesson.getClassSessionId());
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        academicAccessService.requireAccessClassroom(classroomId);
        if (principal.role() == AccountRole.STUDENT && lesson.getStatus() == LessonStatus.DRAFT) {
            throw new NotFoundException("Không tìm thấy nội dung bài học.");
        }
        return toResponse(lesson);
    }

    @Transactional(readOnly = true)
    public Page<LessonRecordResponse> search(
            Long classroomId,
            LessonStatus status,
            String keyword,
            int page,
            int size
    ) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        Long teacherId = null;
        if (principal.role() == AccountRole.TEACHER) {
            teacherId = principal.teacherId();
            if (classroomId != null) {
                academicAccessService.requireManageClassroom(classroomId);
            }
        } else if (principal.role() == AccountRole.STUDENT) {
            throw new BusinessException("Học viên vui lòng dùng API /api/me/lessons.");
        } else if (classroomId != null) {
            academicAccessService.requireAccessClassroom(classroomId);
        }
        return lessonRecordRepository
                .search(classroomId, status, blankToNull(keyword), teacherId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<LessonRecordResponse> listForCurrentStudent() {
        Long studentId = SecurityUtils.requireLinkedStudentId();
        return lessonRecordRepository
                .findPublishedForStudent(studentId, List.of(LessonStatus.PUBLISHED, LessonStatus.COMPLETED))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private LessonRecord require(Long id) {
        return lessonRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nội dung bài học."));
    }

    private LessonRecordResponse toResponse(LessonRecord lesson) {
        return new LessonRecordResponse(
                lesson.getId(),
                lesson.getClassSessionId(),
                academicAccessService.classroomIdOfSession(lesson.getClassSessionId()),
                lesson.getTitle(),
                lesson.getObjectives(),
                lesson.getPlannedContent(),
                lesson.getActualContent(),
                lesson.getVocabulary(),
                lesson.getGrammarTopics(),
                lesson.getSkills(),
                lesson.getHomeworkInstruction(),
                lesson.getTeacherNote(),
                lesson.getStatus(),
                lesson.getCreatedBy(),
                lesson.getCreatedAt(),
                lesson.getUpdatedBy(),
                lesson.getUpdatedAt()
        );
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
