package com.englishcenter.enrollment;

import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentEligibilityService {
    public static final String NOT_ACTIVE_ON_SESSION_DATE_MESSAGE =
            "Học viên không ở trạng thái đang học tại ngày của buổi học này.";
    public static final String EFFECTIVE_DATE_REQUIRED_INACTIVE_MESSAGE =
            "Vui lòng chọn ngày bắt đầu nghỉ.";
    public static final String EFFECTIVE_DATE_REQUIRED_REACTIVATE_MESSAGE =
            "Vui lòng chọn ngày bắt đầu học lại.";
    public static final String REACTIVATE_INVALID_DATE_MESSAGE =
            "Ngày bắt đầu học lại không trùng với lịch học của lớp.\n"
                    + "Vui lòng chọn một ngày học hợp lệ.";
    public static final String REACTIVATE_BEFORE_INACTIVE_MESSAGE =
            "Ngày bắt đầu học lại phải sau ngày bắt đầu nghỉ.";
    public static final String REACTIVATE_OVERLAP_MESSAGE =
            "Ngày bắt đầu học lại bị trùng với một khoảng trạng thái đã có.";
    public static final String LEARNING_START_DATE_REQUIRED_MESSAGE =
            "Vui lòng chọn ngày bắt đầu học.";
    public static final String PAUSE_STATUS_INVALID_MESSAGE =
            "Trạng thái tạm nghỉ phải là Bảo lưu hoặc Ngừng học.";

    private static final DateTimeFormatter VIETNAMESE_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentStatusHistoryRepository statusHistoryRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassSessionRepository classSessionRepository;

    public EnrollmentEligibilityService(
            EnrollmentRepository enrollmentRepository,
            EnrollmentStatusHistoryRepository statusHistoryRepository,
            AttendanceRepository attendanceRepository,
            ClassSessionRepository classSessionRepository
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.attendanceRepository = attendanceRepository;
        this.classSessionRepository = classSessionRepository;
    }

    @Transactional(readOnly = true)
    public boolean isActiveOnDate(Long enrollmentId, LocalDate date) {
        return isActiveOnDate(findEnrollment(enrollmentId), date);
    }

    public boolean isActiveOnDate(Enrollment enrollment, LocalDate date) {
        if (enrollment == null || date == null || enrollment.getStartDate() == null) {
            return false;
        }
        if (date.isBefore(enrollment.getStartDate())) {
            return false;
        }
        return statusHistoryRepository.isActiveAt(enrollment.getId(), date);
    }

    @Transactional(readOnly = true)
    public Optional<LocalDate> getLatestAttendanceDate(Long enrollmentId) {
        return getLatestAttendanceDate(findEnrollment(enrollmentId));
    }

    public Optional<LocalDate> getLatestAttendanceDate(Enrollment enrollment) {
        return attendanceRepository.findLatestValidAttendanceDate(
                enrollment.getStudent().getId(),
                enrollment.getClassroom().getId(),
                enrollment.getStartDate()
        );
    }

    public Optional<LocalDate> getEarliestValidAttendanceDate(Enrollment enrollment) {
        return attendanceRepository.findEarliestValidAttendanceDate(
                enrollment.getStudent().getId(),
                enrollment.getClassroom().getId()
        );
    }

    @Transactional(readOnly = true)
    public void validateInactiveEffectiveDate(Long enrollmentId, LocalDate effectiveDate) {
        validateInactiveEffectiveDate(findEnrollment(enrollmentId), effectiveDate);
    }

    public void validateInactiveEffectiveDate(Enrollment enrollment, LocalDate effectiveDate) {
        if (effectiveDate == null) {
            throw new BusinessException(EFFECTIVE_DATE_REQUIRED_INACTIVE_MESSAGE);
        }

        Optional<LocalDate> latestAttendanceDate = getLatestAttendanceDate(enrollment);
        if (latestAttendanceDate.isPresent() && !effectiveDate.isAfter(latestAttendanceDate.get())) {
            throw new BusinessException(inactiveDateAfterAttendanceMessage(latestAttendanceDate.get()));
        }
    }

    @Transactional(readOnly = true)
    public void validateReactivateEffectiveDate(Long enrollmentId, LocalDate effectiveDate) {
        validateReactivateEffectiveDate(findEnrollment(enrollmentId), effectiveDate);
    }

    public void validateReactivateEffectiveDate(Enrollment enrollment, LocalDate effectiveDate) {
        if (effectiveDate == null) {
            throw new BusinessException(EFFECTIVE_DATE_REQUIRED_REACTIVATE_MESSAGE);
        }

        List<EnrollmentStatusHistory> histories = statusHistoryRepository
                .findByEnrollmentIdOrderByEffectiveFromAscIdAsc(enrollment.getId());
        EnrollmentStatusHistory current = currentOpenPeriod(histories)
                .orElseThrow(() -> new BusinessException("Enrollment status history is missing"));

        if (!effectiveDate.isAfter(current.getEffectiveFrom())) {
            throw new BusinessException(REACTIVATE_BEFORE_INACTIVE_MESSAGE);
        }

        if (overlapsExistingPeriods(histories, current, effectiveDate)) {
            throw new BusinessException(REACTIVATE_OVERLAP_MESSAGE);
        }

        Classroom classroom = enrollment.getClassroom();
        if (effectiveDate.isBefore(classroom.getStartDate())) {
            throw new BusinessException(REACTIVATE_INVALID_DATE_MESSAGE);
        }

        if (classSessionRepository.countByClassroomId(classroom.getId()) > 0) {
            if (!classSessionRepository.existsByClassroomIdAndSessionDateAndStatusNot(
                    classroom.getId(),
                    effectiveDate,
                    ClassSessionStatus.CANCELED
            )) {
                throw new BusinessException(REACTIVATE_INVALID_DATE_MESSAGE);
            }
            return;
        }

        if (!EnrollmentLearningDateHelper.isValidLearningDate(classroom, effectiveDate)) {
            throw new BusinessException(REACTIVATE_INVALID_DATE_MESSAGE);
        }
    }

    public void validateLearningStartDateChange(Enrollment enrollment, LocalDate newStartDate) {
        if (newStartDate == null) {
            throw new BusinessException(LEARNING_START_DATE_REQUIRED_MESSAGE);
        }

        Classroom classroom = enrollment.getClassroom();
        EnrollmentLearningDateHelper.validateLearningStartDate(classroom, newStartDate);
        validateLearningStartMatchesGeneratedSession(classroom, newStartDate);

        Optional<LocalDate> firstPeriodEnd = firstPeriodEndDate(enrollment);
        if (firstPeriodEnd.isPresent() && !newStartDate.isBefore(firstPeriodEnd.get())) {
            throw new BusinessException(learningStartBeforeInactiveMessage(firstPeriodEnd.get()));
        }

        Optional<LocalDate> earliestValidAttendanceDate = getEarliestValidAttendanceDate(enrollment);
        if (earliestValidAttendanceDate.isPresent()
                && newStartDate.isAfter(earliestValidAttendanceDate.get())) {
            throw new BusinessException(learningStartAfterAttendanceMessage(earliestValidAttendanceDate.get()));
        }
    }

    public Optional<LocalDate> firstPeriodEndDate(Enrollment enrollment) {
        List<EnrollmentStatusHistory> histories = statusHistoryRepository
                .findByEnrollmentIdOrderByEffectiveFromAscIdAsc(enrollment.getId());
        if (histories.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(histories.getFirst().getEffectiveTo());
    }

    public static String inactiveDateAfterAttendanceMessage(LocalDate latestAttendanceDate) {
        String formatted = latestAttendanceDate.format(VIETNAMESE_DATE);
        return "Học viên đã có dữ liệu điểm danh đến ngày " + formatted + ".\n"
                + "Ngày bắt đầu nghỉ phải sau ngày này.\n\n"
                + "Nếu muốn nghỉ từ ngày sớm hơn, vui lòng xóa hoặc điều chỉnh các dữ liệu điểm danh "
                + "phát sinh sau ngày nghỉ trước.";
    }

    public static String learningStartAfterAttendanceMessage(LocalDate earliestValidAttendanceDate) {
        String formatted = earliestValidAttendanceDate.format(VIETNAMESE_DATE);
        return "Học viên đã có điểm danh còn hiệu lực từ ngày " + formatted + ".\n"
                + "Ngày bắt đầu học không được sau ngày này.\n\n"
                + "Nếu muốn dời ngày bắt đầu học sang sau, vui lòng hoàn tác hoặc điều chỉnh các buổi "
                + "điểm danh còn hiệu lực trước ngày bắt đầu học mới.";
    }

    public static String learningStartBeforeInactiveMessage(LocalDate firstPeriodEndDate) {
        return "Ngày bắt đầu học phải trước ngày bắt đầu nghỉ ("
                + firstPeriodEndDate.format(VIETNAMESE_DATE)
                + ").";
    }

    private void validateLearningStartMatchesGeneratedSession(Classroom classroom, LocalDate date) {
        if (classSessionRepository.countByClassroomId(classroom.getId()) == 0) {
            return;
        }
        if (!classSessionRepository.existsByClassroomIdAndSessionDateAndStatusNot(
                classroom.getId(),
                date,
                ClassSessionStatus.CANCELED
        )) {
            throw new BusinessException(EnrollmentLearningDateHelper.LEARNING_START_MUST_MATCH_SESSION_MESSAGE);
        }
    }

    private Optional<EnrollmentStatusHistory> currentOpenPeriod(List<EnrollmentStatusHistory> histories) {
        Optional<EnrollmentStatusHistory> open = histories.stream()
                .filter(history -> history.getEffectiveTo() == null)
                .reduce((first, second) -> second);
        if (open.isPresent()) {
            return open;
        }
        return histories.isEmpty() ? Optional.empty() : Optional.of(histories.getLast());
    }

    private boolean overlapsExistingPeriods(
            List<EnrollmentStatusHistory> histories,
            EnrollmentStatusHistory current,
            LocalDate effectiveDate
    ) {
        for (EnrollmentStatusHistory history : histories) {
            if (isSamePeriod(history, current)) {
                continue;
            }
            LocalDate to = history.getEffectiveTo();
            if (to == null || effectiveDate.isBefore(to)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSamePeriod(EnrollmentStatusHistory left, EnrollmentStatusHistory right) {
        if (left == right) {
            return true;
        }
        return left.getId() != null && left.getId().equals(right.getId());
    }

    private Enrollment findEnrollment(Long enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found"));
    }
}
