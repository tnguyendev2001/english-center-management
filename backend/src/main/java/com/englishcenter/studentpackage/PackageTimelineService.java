package com.englishcenter.studentpackage;

import com.englishcenter.attendance.Attendance;
import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentEligibilityService;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentSessionService;
import com.englishcenter.studentpackage.dto.StudentPackagePeriodResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PackageTimelineService {
    private final StudentPackageRepository studentPackageRepository;
    private final StudentPackagePeriodAdjustmentRepository adjustmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentEligibilityService enrollmentEligibilityService;
    private final EnrollmentSessionService enrollmentSessionService;

    public PackageTimelineService(
            StudentPackageRepository studentPackageRepository,
            StudentPackagePeriodAdjustmentRepository adjustmentRepository,
            EnrollmentRepository enrollmentRepository,
            ClassSessionRepository classSessionRepository,
            AttendanceRepository attendanceRepository,
            EnrollmentEligibilityService enrollmentEligibilityService,
            EnrollmentSessionService enrollmentSessionService
    ) {
        this.studentPackageRepository = studentPackageRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.classSessionRepository = classSessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.enrollmentEligibilityService = enrollmentEligibilityService;
        this.enrollmentSessionService = enrollmentSessionService;
    }

    @Transactional(readOnly = true)
    public StudentPackagePeriodResponse getPeriod(Long studentPackageId) {
        StudentPackage studentPackage = studentPackageRepository.findWithRelationsById(studentPackageId)
                .orElseThrow(() -> new NotFoundException("Student package not found"));
        return toResponse(studentPackage);
    }

    @Transactional
    public void markNeedsRecalculation(Long enrollmentId) {
        studentPackageRepository.markPeriodNeedsRecalculationByEnrollmentId(enrollmentId);
    }

    @Transactional
    public List<StudentPackagePeriodResponse> recalculatePackageTimeline(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found"));
        List<StudentPackage> packages =
                studentPackageRepository.findByEnrollmentIdOrderByCycleNoAscIdAsc(enrollmentId);
        List<ClassSession> consumingSessions = attendanceRepository
                .findValidByStudentIdAndClassroomId(
                        enrollment.getStudent().getId(),
                        enrollment.getClassroom().getId()
                )
                .stream()
                .filter(attendance -> enrollmentSessionService.consumesSession(
                        attendance,
                        attendance.getSession(),
                        enrollment
                ))
                .map(Attendance::getSession)
                .toList();

        int sessionOffset = 0;
        for (StudentPackage studentPackage : packages) {
            int packageSessions = studentPackage.getTotalSessions();
            LocalDate calculatedStart = sessionOffset < consumingSessions.size()
                    ? consumingSessions.get(sessionOffset).getSessionDate()
                    : null;
            int endIndex = sessionOffset + packageSessions - 1;
            LocalDate calculatedEnd = endIndex < consumingSessions.size()
                    ? consumingSessions.get(endIndex).getSessionDate()
                    : null;

            studentPackage.setCalculatedPeriodStartDate(calculatedStart);
            studentPackage.setCalculatedPeriodEndDate(calculatedEnd);
            studentPackage.setPeriodNeedsRecalculation(false);
            sessionOffset += packageSessions;
        }
        studentPackageRepository.saveAll(packages);

        return packages.stream().map(this::toResponse).toList();
    }

    @Transactional
    public StudentPackagePeriodResponse adjustPeriodStart(
            Long studentPackageId,
            LocalDate newDate,
            String reason,
            String changedBy
    ) {
        StudentPackage studentPackage = studentPackageRepository.findWithRelationsForUpdateById(studentPackageId)
                .orElseThrow(() -> new NotFoundException("Student package not found"));
        String normalizedReason = requireText(reason, "Adjustment reason is required");
        String normalizedChangedBy = requireText(changedBy, "Changed by is required");
        validateManualStart(studentPackage, newDate);

        StudentPackagePeriodAdjustment adjustment = new StudentPackagePeriodAdjustment();
        adjustment.setStudentPackage(studentPackage);
        adjustment.setOldEffectiveStartDate(studentPackage.getEffectivePeriodStartDate());
        adjustment.setNewManualStartDate(newDate);
        adjustment.setReason(normalizedReason);
        adjustment.setChangedAt(LocalDateTime.now());
        adjustment.setChangedBy(normalizedChangedBy);

        studentPackage.setManualPeriodStartDate(newDate);
        studentPackageRepository.save(studentPackage);
        adjustment = adjustmentRepository.save(adjustment);
        return toResponse(studentPackage, adjustment);
    }

    private void validateManualStart(StudentPackage studentPackage, LocalDate newDate) {
        if (newDate == null) {
            throw new BusinessException("Period start date is required");
        }

        Enrollment enrollment = studentPackage.getEnrollment();
        if (newDate.isBefore(enrollment.getStartDate())) {
            throw new BusinessException("Ngày bắt đầu kỳ không được trước ngày bắt đầu học của ghi danh");
        }
        if (enrollment.getEndDate() != null && newDate.isAfter(enrollment.getEndDate())) {
            throw new BusinessException("Ngày bắt đầu kỳ nằm ngoài thời gian ghi danh");
        }
        boolean validSessionDate = classSessionRepository
                .findByClassroomIdAndSessionDateOrderByStartTimeAscIdAsc(
                        studentPackage.getClassroom().getId(),
                        newDate
                )
                .stream()
                .anyMatch(session -> session.getStatus() != ClassSessionStatus.CANCELED);
        if (!validSessionDate) {
            throw new BusinessException("Ngày bắt đầu kỳ phải là một buổi học chưa bị hủy của lớp");
        }
        if (!enrollmentEligibilityService.isActiveOnDate(enrollment, newDate)) {
            throw new BusinessException("Ngày bắt đầu kỳ không được nằm trong thời gian ngừng học hoặc bảo lưu");
        }
        if (studentPackage.getCalculatedPeriodEndDate() != null
                && newDate.isAfter(studentPackage.getCalculatedPeriodEndDate())) {
            throw new BusinessException("Ngày bắt đầu kỳ không được sau ngày kết thúc kỳ");
        }

        List<StudentPackage> packages =
                studentPackageRepository.findByEnrollmentIdOrderByCycleNoAscIdAsc(enrollment.getId());
        int currentIndex = indexOf(packages, studentPackage.getId());
        if (currentIndex > 0) {
            StudentPackage previous = packages.get(currentIndex - 1);
            if (previous.getCalculatedPeriodEndDate() == null) {
                throw new BusinessException("Cần tính lại kỳ học phí trước đó trước khi điều chỉnh");
            }
            if (!newDate.isAfter(previous.getCalculatedPeriodEndDate())) {
                throw new BusinessException("Ngày bắt đầu kỳ bị chồng lấn với kỳ học phí trước");
            }
        }
        if (currentIndex >= 0 && currentIndex < packages.size() - 1) {
            LocalDate nextStart = packages.get(currentIndex + 1).getEffectivePeriodStartDate();
            if (nextStart != null && !newDate.isBefore(nextStart)) {
                throw new BusinessException("Ngày bắt đầu kỳ bị chồng lấn với kỳ học phí tiếp theo");
            }
        }
    }

    private int indexOf(List<StudentPackage> packages, Long studentPackageId) {
        for (int index = 0; index < packages.size(); index++) {
            if (packages.get(index).getId().equals(studentPackageId)) {
                return index;
            }
        }
        throw new NotFoundException("Student package not found in enrollment timeline");
    }

    private StudentPackagePeriodResponse toResponse(StudentPackage studentPackage) {
        StudentPackagePeriodAdjustment latestAdjustment = adjustmentRepository
                .findTopByStudentPackageIdOrderByChangedAtDescIdDesc(studentPackage.getId())
                .orElse(null);
        return toResponse(studentPackage, latestAdjustment);
    }

    private StudentPackagePeriodResponse toResponse(
            StudentPackage studentPackage,
            StudentPackagePeriodAdjustment latestAdjustment
    ) {
        return new StudentPackagePeriodResponse(
                studentPackage.getId(),
                studentPackage.getEnrollment().getId(),
                studentPackage.getCycleNo(),
                studentPackage.getPackageName(),
                studentPackage.getTotalSessions(),
                studentPackage.getCalculatedPeriodStartDate(),
                studentPackage.getCalculatedPeriodEndDate(),
                studentPackage.getManualPeriodStartDate(),
                studentPackage.getEffectivePeriodStartDate(),
                Boolean.TRUE.equals(studentPackage.getPeriodNeedsRecalculation()),
                latestAdjustment == null ? null : latestAdjustment.getReason(),
                latestAdjustment == null ? null : latestAdjustment.getChangedAt(),
                latestAdjustment == null ? null : latestAdjustment.getChangedBy()
        );
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }
}
