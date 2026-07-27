package com.englishcenter.academic.evaluation;

import com.englishcenter.academic.common.AcademicAccessService;
import com.englishcenter.academic.evaluation.dto.CreateEvaluationPeriodRequest;
import com.englishcenter.academic.evaluation.dto.EvaluationPeriodResponse;
import com.englishcenter.academic.evaluation.dto.ReopenEvaluationRequest;
import com.englishcenter.academic.evaluation.dto.UpdateEvaluationPeriodRequest;
import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationPeriodService {
    private final EvaluationPeriodRepository evaluationPeriodRepository;
    private final ClassroomRepository classroomRepository;
    private final AcademicAccessService academicAccessService;

    public EvaluationPeriodService(
            EvaluationPeriodRepository evaluationPeriodRepository,
            ClassroomRepository classroomRepository,
            AcademicAccessService academicAccessService
    ) {
        this.evaluationPeriodRepository = evaluationPeriodRepository;
        this.classroomRepository = classroomRepository;
        this.academicAccessService = academicAccessService;
    }

    @Transactional
    public EvaluationPeriodResponse create(CreateEvaluationPeriodRequest request) {
        requireAdminOrTeacherManage(request.classroomId());
        validateDates(request.startDate(), request.endDate());
        if (request.classroomId() != null && !classroomRepository.existsById(request.classroomId())) {
            throw new NotFoundException("Không tìm thấy lớp học.");
        }
        EvaluationPeriod period = new EvaluationPeriod();
        period.setName(request.name().trim());
        period.setClassroomId(request.classroomId());
        period.setStartDate(request.startDate());
        period.setEndDate(request.endDate());
        period.setStatus(EvaluationPeriodStatus.OPEN);
        return toResponse(evaluationPeriodRepository.save(period));
    }

    @Transactional
    public EvaluationPeriodResponse update(Long id, UpdateEvaluationPeriodRequest request) {
        EvaluationPeriod period = require(id);
        requireAdminOrTeacherManage(period.getClassroomId() != null ? period.getClassroomId() : request.classroomId());
        if (period.getStatus() == EvaluationPeriodStatus.CLOSED) {
            throw new BusinessException("Kỳ đánh giá đã đóng; ADMIN cần mở lại trước khi sửa.");
        }
        validateDates(request.startDate(), request.endDate());
        if (request.classroomId() != null && !classroomRepository.existsById(request.classroomId())) {
            throw new NotFoundException("Không tìm thấy lớp học.");
        }
        period.setName(request.name().trim());
        period.setClassroomId(request.classroomId());
        period.setStartDate(request.startDate());
        period.setEndDate(request.endDate());
        return toResponse(evaluationPeriodRepository.save(period));
    }

    @Transactional
    public EvaluationPeriodResponse close(Long id) {
        EvaluationPeriod period = require(id);
        requireAdminOrTeacherManage(period.getClassroomId());
        period.setStatus(EvaluationPeriodStatus.CLOSED);
        return toResponse(evaluationPeriodRepository.save(period));
    }

    @Transactional
    public EvaluationPeriodResponse reopen(Long id, ReopenEvaluationRequest request) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() != AccountRole.ADMIN) {
            throw new AccessDeniedException("Chỉ ADMIN mới được mở lại kỳ đánh giá.");
        }
        EvaluationPeriod period = require(id);
        if (period.getStatus() != EvaluationPeriodStatus.CLOSED) {
            throw new BusinessException("Chỉ có thể mở lại kỳ đánh giá đã đóng.");
        }
        period.setStatus(EvaluationPeriodStatus.OPEN);
        period.setReopenReason(request.reason().trim());
        return toResponse(evaluationPeriodRepository.save(period));
    }

    @Transactional(readOnly = true)
    public EvaluationPeriodResponse getById(Long id) {
        EvaluationPeriod period = require(id);
        if (period.getClassroomId() != null) {
            academicAccessService.requireAccessClassroom(period.getClassroomId());
        }
        return toResponse(period);
    }

    @Transactional(readOnly = true)
    public Page<EvaluationPeriodResponse> search(
            Long classroomId,
            EvaluationPeriodStatus status,
            String keyword,
            int page,
            int size
    ) {
        if (classroomId != null) {
            academicAccessService.requireAccessClassroom(classroomId);
        }
        return evaluationPeriodRepository
                .search(classroomId, status, blankToNull(keyword), PageRequest.of(page, size))
                .map(this::toResponse);
    }

    public EvaluationPeriod require(Long id) {
        return evaluationPeriodRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ đánh giá."));
    }

    private void requireAdminOrTeacherManage(Long classroomId) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.ADMIN) {
            return;
        }
        if (classroomId == null) {
            throw new AccessDeniedException("Chỉ ADMIN mới tạo kỳ đánh giá toàn trung tâm.");
        }
        academicAccessService.requireManageClassroom(classroomId);
    }

    private void validateDates(java.time.LocalDate start, java.time.LocalDate end) {
        if (end.isBefore(start)) {
            throw new BusinessException("Ngày kết thúc phải từ ngày bắt đầu trở đi.");
        }
    }

    private EvaluationPeriodResponse toResponse(EvaluationPeriod period) {
        return new EvaluationPeriodResponse(
                period.getId(),
                period.getName(),
                period.getClassroomId(),
                period.getStartDate(),
                period.getEndDate(),
                period.getStatus(),
                period.getReopenReason(),
                period.getCreatedAt(),
                period.getUpdatedAt()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
