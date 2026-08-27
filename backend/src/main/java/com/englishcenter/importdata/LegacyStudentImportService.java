package com.englishcenter.importdata;

import com.englishcenter.attendance.Attendance;
import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.attendance.AttendanceStatus;
import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.classsession.ClassSessionService;
import com.englishcenter.classsession.dto.GenerateClassSessionsResponse;
import com.englishcenter.common.config.AppTimeProperties;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentSessionService;
import com.englishcenter.importdata.LegacyExcelWorkbookParser.ParsedRow;
import com.englishcenter.importdata.LegacyExcelWorkbookParser.ParsedSheet;
import com.englishcenter.importdata.LegacyStudentImportWriter.ClassroomEnsureResult;
import com.englishcenter.importdata.LegacyStudentImportWriter.StudentImportResult;
import com.englishcenter.importdata.dto.LegacyImportConfirmResponse;
import com.englishcenter.importdata.dto.LegacyImportPreviewResponse;
import com.englishcenter.importdata.dto.LegacyImportRowAction;
import com.englishcenter.importdata.dto.LegacyImportRowPreview;
import com.englishcenter.importdata.dto.LegacyImportRowStatus;
import com.englishcenter.importdata.dto.LegacyImportSheetPreview;
import com.englishcenter.student.Student;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.tuitionpackage.TuitionPackage;
import com.englishcenter.tuitionpackage.TuitionPackageRepository;
import com.englishcenter.tuitionpackage.TuitionPackageStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LegacyStudentImportService {
    private static final int REQUIRED_PACKAGE_SESSIONS = 8;
    private static final long SUSPICIOUS_DAYS_AFTER_MEDIAN = 60;
    private static final long FUTURE_YEARS_SUSPICIOUS = 1;
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String CONFIRMATION_WARNING =
            "Các buổi lịch sử hợp lệ mặc định là PRESENT; ngày trong hai cột nghỉ sẽ ghi đè thành "
                    + "ABSENT/EXCUSED. Các chu kỳ gói cần thiết sẽ tạo hóa đơn chưa thanh toán.";

    private final LegacyExcelWorkbookParser workbookParser;
    private final TuitionPackageRepository tuitionPackageRepository;
    private final ClassroomRepository classroomRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassSessionService classSessionService;
    private final EnrollmentSessionService enrollmentSessionService;
    private final LegacyStudentImportWriter importWriter;
    private final AppTimeProperties appTimeProperties;

    public LegacyStudentImportService(
            LegacyExcelWorkbookParser workbookParser,
            TuitionPackageRepository tuitionPackageRepository,
            ClassroomRepository classroomRepository,
            StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository,
            ClassSessionRepository classSessionRepository,
            AttendanceRepository attendanceRepository,
            ClassSessionService classSessionService,
            EnrollmentSessionService enrollmentSessionService,
            LegacyStudentImportWriter importWriter,
            AppTimeProperties appTimeProperties
    ) {
        this.workbookParser = workbookParser;
        this.tuitionPackageRepository = tuitionPackageRepository;
        this.classroomRepository = classroomRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.classSessionRepository = classSessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.classSessionService = classSessionService;
        this.enrollmentSessionService = enrollmentSessionService;
        this.importWriter = importWriter;
        this.appTimeProperties = appTimeProperties;
    }

    @Transactional(readOnly = true)
    public LegacyImportPreviewResponse preview(MultipartFile file, Long tuitionPackageId) {
        TuitionPackage tuitionPackage = requireEightSessionPackage(tuitionPackageId);
        List<ParsedSheet> parsedSheets = workbookParser.parse(file);
        Analysis analysis = analyze(parsedSheets, tuitionPackage);
        return toPreviewResponse(tuitionPackage, analysis);
    }

    public LegacyImportConfirmResponse confirm(MultipartFile file, Long tuitionPackageId) {
        TuitionPackage tuitionPackage = requireEightSessionPackage(tuitionPackageId);
        List<ParsedSheet> parsedSheets = workbookParser.parse(file);
        Analysis analysis = analyze(parsedSheets, tuitionPackage);

        if (!analysis.canConfirm()) {
            List<String> hardErrors = new ArrayList<>();
            analysis.sheets().forEach(sheet -> hardErrors.addAll(sheet.errors()));
            analysis.allRows().stream()
                    .filter(row -> row.status() == LegacyImportRowStatus.INVALID)
                    .forEach(row -> hardErrors.addAll(row.errors()));
            throw new BusinessException(
                    "Import validation failed. Fix hard errors before confirming. "
                            + String.join("; ", hardErrors.stream().distinct().limit(5).toList())
            );
        }

        int classroomsCreated = 0;
        int classroomsReused = 0;
        int studentsCreated = 0;
        int studentsReused = 0;
        int enrollmentsCreated = 0;
        int enrollmentsSkipped = 0;
        int sessionsCreated = 0;
        int sessionsReused = 0;
        int attendancesCreated = 0;
        int invoicesCreated = 0;
        int packageCyclesCreated = 0;
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        Map<String, Classroom> classroomByKey = new HashMap<>();
        Map<String, Student> studentsByPhone = new HashMap<>();
        Set<Long> countedClassroomIds = new HashSet<>();
        Set<Long> countedStudentIds = new HashSet<>();

        for (LegacyImportSheetPreview sheet : analysis.sheets()) {
            warnings.addAll(sheet.warnings());
            try {
                ClassroomEnsureResult classroomResult = importWriter.ensureClassroom(
                        sheet,
                        tuitionPackage,
                        classroomByKey
                );
                if (countedClassroomIds.add(classroomResult.classroom().getId())) {
                    if (classroomResult.created()) {
                        classroomsCreated++;
                    } else {
                        classroomsReused++;
                    }
                }

                GenerateClassSessionsResponse sessionResult = importWriter.generateSessions(
                        classroomResult.classroom()
                );
                sessionsCreated += sessionResult.createdCount();
                sessionsReused += sessionResult.skippedCount();

                for (LegacyImportRowPreview row : sheet.rows()) {
                    warnings.addAll(row.warnings());
                    if (row.status() == LegacyImportRowStatus.INVALID) {
                        continue;
                    }
                    if (row.status() == LegacyImportRowStatus.SKIPPED_DUPLICATE_ENROLLMENT
                            || row.actions().contains(LegacyImportRowAction.SKIP_DUPLICATE)) {
                        enrollmentsSkipped++;
                        continue;
                    }

                    try {
                        StudentImportResult studentResult = importWriter.importStudent(
                                classroomResult.classroom(),
                                tuitionPackage,
                                row,
                                studentsByPhone
                        );
                        if (countedStudentIds.add(studentResult.student().getId())) {
                            if (studentResult.studentCreated()) {
                                studentsCreated++;
                            } else {
                                studentsReused++;
                            }
                        } else {
                            studentsReused++;
                        }

                        if (studentResult.enrollmentSkipped()) {
                            enrollmentsSkipped++;
                        } else {
                            enrollmentsCreated++;
                            attendancesCreated += studentResult.attendancesCreated();
                            invoicesCreated += studentResult.invoicesCreated();
                            packageCyclesCreated += studentResult.packageCyclesCreated();
                        }
                    } catch (Exception ex) {
                        errors.add(sheet.sheetName() + " / " + row.studentName() + ": " + ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                errors.add(sheet.sheetName() + ": " + ex.getMessage());
            }
        }

        return new LegacyImportConfirmResponse(
                classroomsCreated,
                classroomsReused,
                studentsCreated,
                studentsReused,
                enrollmentsCreated,
                enrollmentsSkipped,
                sessionsCreated,
                sessionsReused,
                attendancesCreated,
                invoicesCreated,
                packageCyclesCreated,
                warnings.stream().distinct().toList(),
                errors
        );
    }

    private Analysis analyze(List<ParsedSheet> parsedSheets, TuitionPackage tuitionPackage) {
        LocalDate today = businessDate();
        List<LegacyImportSheetPreview> sheets = new ArrayList<>();
        List<LegacyImportRowPreview> allRows = new ArrayList<>();
        Set<String> seenClassroomKeys = new LinkedHashSet<>();
        int newClassrooms = 0;
        int existingClassrooms = 0;
        int newStudents = 0;
        int existingStudents = 0;
        int newEnrollments = 0;
        int duplicateEnrollments = 0;
        int validRows = 0;
        int warningRows = 0;
        int invalidRows = 0;

        Map<String, Long> phoneResolution = new HashMap<>();

        for (ParsedSheet parsed : parsedSheets) {
            String classKey = LegacyImportNormalizer.normalizeClassroomNameKey(parsed.sheetName());
            String classCode = LegacyImportNormalizer.toClassCode(parsed.sheetName());
            Classroom existingClassroom = findExistingClassroom(classCode, classKey).orElse(null);
            boolean classroomExists = existingClassroom != null;
            if (seenClassroomKeys.add(classKey)) {
                if (classroomExists) {
                    existingClassrooms++;
                } else {
                    newClassrooms++;
                }
            }

            List<String> sheetErrors = new ArrayList<>(parsed.errors());
            List<String> sheetWarnings = new ArrayList<>(parsed.warnings());
            if (parsed.classroomStartDate() != null
                    && !parsed.daysOfWeek().isEmpty()
                    && !ClassDayOfWeek.isDateMatchingDaysOfWeek(parsed.classroomStartDate(), parsed.daysOfWeek())) {
                sheetWarnings.add("classroomStartDate does not match daysOfWeek");
            }

            List<SessionCandidate> sessionCandidates = List.of();
            int existingSessionCount = 0;
            int sessionsToCreate = 0;
            LocalDate firstSessionDate = null;
            LocalDate lastGeneratedSessionDate = null;
            if (parsed.classroomStartDate() != null && !parsed.daysOfWeek().isEmpty()) {
                SessionPlan sessionPlan = buildSessionPlan(parsed, existingClassroom, today);
                sessionCandidates = sessionPlan.candidates();
                existingSessionCount = sessionPlan.existingSessionCount();
                sessionsToCreate = sessionPlan.sessionsToCreate();
                firstSessionDate = sessionPlan.firstSessionDate();
                lastGeneratedSessionDate = sessionPlan.lastSessionDate();
            }

            List<LocalDate> learningDates = parsed.rows().stream()
                    .map(ParsedRow::learningStartDate)
                    .filter(date -> date != null)
                    .sorted()
                    .toList();
            LocalDate medianLearningDate = medianDate(learningDates);

            List<LegacyImportRowPreview> rowPreviews = new ArrayList<>();
            for (ParsedRow parsedRow : parsed.rows()) {
                LegacyImportRowPreview rowPreview = analyzeRow(
                        parsed,
                        parsedRow,
                        existingClassroom,
                        sessionCandidates,
                        today,
                        tuitionPackage.getPrice(),
                        medianLearningDate,
                        phoneResolution
                );
                rowPreviews.add(rowPreview);
                allRows.add(rowPreview);

                if (rowPreview.status() == LegacyImportRowStatus.INVALID) {
                    invalidRows++;
                } else if (rowPreview.status() == LegacyImportRowStatus.WARNING
                        || rowPreview.status() == LegacyImportRowStatus.SKIPPED_DUPLICATE_ENROLLMENT) {
                    warningRows++;
                } else {
                    validRows++;
                }

                if (rowPreview.actions().contains(LegacyImportRowAction.CREATE_STUDENT)) {
                    newStudents++;
                } else if (rowPreview.actions().contains(LegacyImportRowAction.REUSE_STUDENT)) {
                    existingStudents++;
                }
                if (rowPreview.actions().contains(LegacyImportRowAction.CREATE_ENROLLMENT)) {
                    newEnrollments++;
                }
                if (rowPreview.actions().contains(LegacyImportRowAction.SKIP_DUPLICATE)
                        || rowPreview.status() == LegacyImportRowStatus.SKIPPED_DUPLICATE_ENROLLMENT) {
                    duplicateEnrollments++;
                }
            }

            sheets.add(new LegacyImportSheetPreview(
                    parsed.sheetIndex(),
                    parsed.sheetName(),
                    LegacyImportNormalizer.normalizeClassroomName(parsed.sheetName()),
                    classCode,
                    parsed.classroomStartDate(),
                    parsed.daysOfWeek(),
                    classroomExists,
                    existingClassroom != null ? existingClassroom.getId() : null,
                    existingSessionCount,
                    sessionsToCreate,
                    firstSessionDate,
                    lastGeneratedSessionDate,
                    sheetErrors,
                    sheetWarnings,
                    rowPreviews
            ));
        }

        boolean sheetHasHardError = sheets.stream().anyMatch(sheet -> !sheet.errors().isEmpty());
        boolean canConfirm = invalidRows == 0
                && !sheetHasHardError
                && (newEnrollments > 0 || duplicateEnrollments > 0);

        return new Analysis(
                sheets,
                allRows,
                newClassrooms,
                existingClassrooms,
                newStudents,
                existingStudents,
                newEnrollments,
                duplicateEnrollments,
                validRows,
                warningRows,
                invalidRows,
                canConfirm
        );
    }

    private LegacyImportRowPreview analyzeRow(
            ParsedSheet sheet,
            ParsedRow row,
            Classroom existingClassroom,
            List<SessionCandidate> sessionCandidates,
            LocalDate today,
            BigDecimal packagePrice,
            LocalDate medianLearningDate,
            Map<String, Long> phoneResolution
    ) {
        List<String> errors = new ArrayList<>(row.attendanceErrors());
        List<String> warnings = new ArrayList<>(row.attendanceWarnings());
        List<LegacyImportRowAction> actions = new ArrayList<>();

        if (row.studentName() == null) {
            errors.add("studentName is required");
        }
        if (row.learningStartDate() == null) {
            if (row.rawLearningStartDate() == null) {
                errors.add("learningStartDate is required");
            } else {
                errors.add("Ngày bắt đầu học \"" + row.rawLearningStartDate() + "\" không hợp lệ.");
            }
        }
        if (sheet.classroomStartDate() == null) {
            errors.add("classroomStartDate is missing for sheet");
        }
        if (sheet.daysOfWeek().isEmpty()) {
            errors.add("daysOfWeek is missing for sheet");
        }
        if (row.phone() == null) {
            warnings.add("Phone is missing; re-import matching uses classroom, name, and learning start date");
        }

        if (row.learningStartDate() != null && sheet.classroomStartDate() != null
                && row.learningStartDate().isBefore(sheet.classroomStartDate())) {
            errors.add("learningStartDate must not be before classroomStartDate");
        }
        if (row.learningStartDate() != null && !sheet.daysOfWeek().isEmpty()
                && !ClassDayOfWeek.isDateMatchingDaysOfWeek(row.learningStartDate(), sheet.daysOfWeek())) {
            warnings.add("learningStartDate does not match classroom daysOfWeek");
        }
        if (row.learningStartDate() != null) {
            if (row.learningStartDate().isAfter(today.plusYears(FUTURE_YEARS_SUSPICIOUS))) {
                warnings.add("learningStartDate is far in the future; possible year-entry mistake");
            }
            if (medianLearningDate != null
                    && ChronoUnit.DAYS.between(medianLearningDate, row.learningStartDate())
                    > SUSPICIOUS_DAYS_AFTER_MEDIAN) {
                warnings.add("learningStartDate is much later than other students in this class");
            }
        }

        Set<LocalDate> availableSessionDates = sessionCandidates.stream()
                .map(SessionCandidate::date)
                .collect(java.util.stream.Collectors.toSet());
        Set<LocalDate> overlappingAbsences = new LinkedHashSet<>(row.absentDates());
        overlappingAbsences.retainAll(row.excusedDates());
        for (LocalDate date : overlappingAbsences) {
            errors.add("Ngày " + formatDate(date)
                    + " xuất hiện đồng thời ở \"Nghỉ không phép\" và \"Xin phép\". "
                    + "Vui lòng chỉ chọn một trạng thái.");
        }
        validateAttendanceDates(
                row.absentDates(),
                "Nghỉ không phép",
                "nghỉ không phép",
                row.learningStartDate(),
                today,
                availableSessionDates,
                errors
        );
        validateAttendanceDates(
                row.excusedDates(),
                "Xin phép",
                "xin phép",
                row.learningStartDate(),
                today,
                availableSessionDates,
                errors
        );

        List<SessionCandidate> eligibleSessions = List.of();
        if (row.learningStartDate() != null) {
            eligibleSessions = sessionCandidates.stream()
                    .filter(candidate -> !candidate.date().isBefore(row.learningStartDate())
                            && !candidate.date().isAfter(today))
                    .toList();
        }
        Set<LocalDate> absentDates = Set.copyOf(row.absentDates());
        Set<LocalDate> excusedDates = Set.copyOf(row.excusedDates());
        Map<SessionCandidate, AttendanceStatus> plannedAttendance = new LinkedHashMap<>();
        for (SessionCandidate session : eligibleSessions) {
            AttendanceStatus status = AttendanceStatus.PRESENT;
            if (absentDates.contains(session.date())) {
                status = AttendanceStatus.ABSENT;
            } else if (excusedDates.contains(session.date())) {
                status = AttendanceStatus.EXCUSED;
            }
            plannedAttendance.put(session, status);
        }

        List<LocalDate> presentDates = attendanceDates(plannedAttendance, AttendanceStatus.PRESENT);
        List<LocalDate> plannedAbsentDates = attendanceDates(plannedAttendance, AttendanceStatus.ABSENT);
        List<LocalDate> plannedExcusedDates = attendanceDates(plannedAttendance, AttendanceStatus.EXCUSED);
        int consumingSessionCount = (int) plannedAttendance.values().stream()
                .filter(enrollmentSessionService::consumesStatus)
                .count();
        int eligibleSessionCount = plannedAttendance.size();
        int packageCycles = LegacyImportCycleCalculator.packageCycles(consumingSessionCount);
        int totalSessions = LegacyImportCycleCalculator.totalSessions(packageCycles);
        int usedSessions = consumingSessionCount;
        int remainingSessions = LegacyImportCycleCalculator.remainingSessions(packageCycles, consumingSessionCount);
        BigDecimal totalDebt = packagePrice.multiply(BigDecimal.valueOf(packageCycles));

        Long existingStudentId = null;
        boolean reuseFromWorkbook = false;
        if (row.phone() != null) {
            if (phoneResolution.containsKey(row.phone())) {
                Long resolved = phoneResolution.get(row.phone());
                if (resolved != null && resolved > 0) {
                    existingStudentId = resolved;
                } else {
                    reuseFromWorkbook = true;
                }
            } else {
                Optional<Student> byPhone = studentRepository.findFirstByNormalizedPhone(row.phone());
                if (byPhone.isPresent()) {
                    existingStudentId = byPhone.get().getId();
                    phoneResolution.put(row.phone(), existingStudentId);
                } else {
                    phoneResolution.put(row.phone(), -1L);
                }
            }
        }
        if (existingStudentId == null
                && row.phone() == null
                && row.studentName() != null
                && row.learningStartDate() != null
                && existingClassroom != null) {
            List<Enrollment> matchingEnrollments = enrollmentRepository
                    .findByClassroomIdOrderByStartDateDescIdDesc(existingClassroom.getId())
                    .stream()
                    .filter(enrollment -> enrollment.getStartDate().equals(row.learningStartDate()))
                    .filter(enrollment -> {
                        String existingName = LegacyImportNormalizer.normalizePersonName(
                                enrollment.getStudent().getFullName()
                        );
                        return existingName != null && existingName.equalsIgnoreCase(row.studentName());
                    })
                    .toList();
            if (matchingEnrollments.size() == 1) {
                existingStudentId = matchingEnrollments.getFirst().getStudent().getId();
            } else if (matchingEnrollments.size() > 1) {
                errors.add("Không thể xác định học viên khi SĐT trống: có nhiều bản ghi trùng lớp, tên và ngày bắt đầu học.");
            }
        }

        Long existingEnrollmentId = null;
        boolean duplicateEnrollment = false;
        if (existingStudentId != null && existingClassroom != null) {
            List<Enrollment> existing = enrollmentRepository
                    .findByStudentIdAndClassroomIdOrderByStartDateAscIdAsc(
                            existingStudentId,
                            existingClassroom.getId()
                    );
            if (!existing.isEmpty()) {
                duplicateEnrollment = true;
                existingEnrollmentId = existing.getFirst().getId();
            }
            validateExistingAttendance(existingStudentId, plannedAttendance, errors);
        }

        if (existingStudentId != null || reuseFromWorkbook) {
            actions.add(LegacyImportRowAction.REUSE_STUDENT);
            if (reuseFromWorkbook) {
                warnings.add("Will reuse student created earlier in this import (same phone)");
            }
        } else if (row.studentName() != null) {
            actions.add(LegacyImportRowAction.CREATE_STUDENT);
        }

        if (duplicateEnrollment) {
            actions.add(LegacyImportRowAction.SKIP_DUPLICATE);
        } else if (errors.isEmpty() && row.studentName() != null && row.learningStartDate() != null) {
            actions.add(LegacyImportRowAction.CREATE_ENROLLMENT);
        }

        LegacyImportRowStatus status;
        if (!errors.isEmpty()) {
            status = LegacyImportRowStatus.INVALID;
        } else if (duplicateEnrollment) {
            status = LegacyImportRowStatus.SKIPPED_DUPLICATE_ENROLLMENT;
        } else if (!warnings.isEmpty()) {
            status = LegacyImportRowStatus.WARNING;
        } else {
            status = LegacyImportRowStatus.VALID;
        }

        return new LegacyImportRowPreview(
                sheet.sheetIndex(),
                sheet.sheetName(),
                row.excelRowNumber(),
                row.studentName(),
                row.phone(),
                row.learningStartDate(),
                sheet.classroomStartDate(),
                eligibleSessionCount,
                presentDates.size(),
                plannedAbsentDates.size(),
                plannedExcusedDates.size(),
                consumingSessionCount,
                presentDates,
                plannedAbsentDates,
                plannedExcusedDates,
                packageCycles,
                totalSessions,
                usedSessions,
                remainingSessions,
                duplicateEnrollment ? 0 : packageCycles,
                totalDebt,
                actions,
                status,
                existingStudentId,
                existingClassroom != null ? existingClassroom.getId() : null,
                existingEnrollmentId,
                errors,
                warnings
        );
    }

    private SessionPlan buildSessionPlan(ParsedSheet parsed, Classroom existingClassroom, LocalDate today) {
        if (existingClassroom == null) {
            LocalDate to = today.isBefore(parsed.classroomStartDate()) ? parsed.classroomStartDate() : today;
            List<LocalDate> plannedDates = classSessionService.plannedSessionDates(
                    parsed.classroomStartDate(),
                    parsed.daysOfWeek(),
                    parsed.classroomStartDate(),
                    to
            );
            List<SessionCandidate> candidates = plannedDates.stream()
                    .filter(date -> !date.isAfter(today))
                    .map(date -> new SessionCandidate(null, date))
                    .toList();
            return new SessionPlan(
                    candidates,
                    0,
                    plannedDates.size(),
                    plannedDates.isEmpty() ? null : plannedDates.getFirst(),
                    plannedDates.isEmpty() ? null : plannedDates.getLast()
            );
        }

        LocalDate from = existingClassroom.getStartDate();
        LocalDate to = today.isBefore(from) ? from : today;
        var generationPlan = classSessionService.planGeneration(existingClassroom, from, to);
        List<ClassSession> existingSessions = classSessionRepository
                .findByClassroomIdOrderBySessionDateAscStartTimeAsc(existingClassroom.getId())
                .stream()
                .filter(session -> !session.getSessionDate().isBefore(from)
                        && !session.getSessionDate().isAfter(to))
                .toList();

        List<SessionCandidate> candidates = new ArrayList<>();
        existingSessions.stream()
                .filter(session -> session.getStatus() != ClassSessionStatus.CANCELED)
                .filter(session -> !session.getSessionDate().isAfter(today))
                .forEach(session -> candidates.add(new SessionCandidate(session.getId(), session.getSessionDate())));

        for (LocalDate plannedDate : generationPlan.plannedDates()) {
            if (plannedDate.isAfter(today)) {
                continue;
            }
            boolean slotAlreadyExists = existingSessions.stream().anyMatch(session ->
                    session.getSessionDate().equals(plannedDate)
                            && session.getStartTime().equals(existingClassroom.getStartTime())
                            && session.getEndTime().equals(existingClassroom.getEndTime()));
            if (!slotAlreadyExists) {
                candidates.add(new SessionCandidate(null, plannedDate));
            }
        }
        candidates.sort(Comparator
                .comparing(SessionCandidate::date)
                .thenComparing(candidate -> candidate.sessionId() == null ? Long.MAX_VALUE : candidate.sessionId()));

        return new SessionPlan(
                List.copyOf(candidates),
                generationPlan.existingSessionCount(),
                generationPlan.sessionsToCreate(),
                generationPlan.firstSessionDate(),
                generationPlan.lastGeneratedSessionDate()
        );
    }

    private void validateAttendanceDates(
            List<LocalDate> dates,
            String columnName,
            String attendanceLabel,
            LocalDate learningStartDate,
            LocalDate businessDate,
            Set<LocalDate> availableSessionDates,
            List<String> errors
    ) {
        for (LocalDate date : dates) {
            if (learningStartDate != null && date.isBefore(learningStartDate)) {
                errors.add("Ngày " + attendanceLabel + " " + formatDate(date)
                        + " nằm trước ngày bắt đầu học " + formatDate(learningStartDate) + ".");
                continue;
            }
            if (date.isAfter(businessDate)) {
                errors.add("Ngày " + formatDate(date) + " trong cột \"" + columnName
                        + "\" nằm sau ngày nghiệp vụ hiện tại " + formatDate(businessDate) + ".");
                continue;
            }
            if (!availableSessionDates.contains(date)) {
                errors.add("Ngày " + formatDate(date) + " trong cột \"" + columnName
                        + "\" không phải là buổi học hợp lệ của lớp.");
            }
        }
    }

    private void validateExistingAttendance(
            Long studentId,
            Map<SessionCandidate, AttendanceStatus> plannedAttendance,
            List<String> errors
    ) {
        for (Map.Entry<SessionCandidate, AttendanceStatus> planned : plannedAttendance.entrySet()) {
            Long sessionId = planned.getKey().sessionId();
            if (sessionId == null) {
                continue;
            }
            Optional<Attendance> existing = attendanceRepository.findBySessionIdAndStudentId(sessionId, studentId);
            if (existing.isPresent()
                    && Boolean.TRUE.equals(existing.get().getValid())
                    && existing.get().getStatus() != planned.getValue()) {
                errors.add("Buổi " + formatDate(planned.getKey().date()) + " đã có điểm danh "
                        + existing.get().getStatus() + " nhưng file Excel yêu cầu " + planned.getValue() + ".");
            }
        }
    }

    private List<LocalDate> attendanceDates(
            Map<SessionCandidate, AttendanceStatus> plannedAttendance,
            AttendanceStatus status
    ) {
        return plannedAttendance.entrySet().stream()
                .filter(entry -> entry.getValue() == status)
                .map(entry -> entry.getKey().date())
                .toList();
    }

    private String formatDate(LocalDate date) {
        return DISPLAY_DATE_FORMATTER.format(date);
    }

    private LocalDate businessDate() {
        return LocalDate.now(appTimeProperties.zoneId());
    }

    private Optional<Classroom> findExistingClassroom(String classCode, String normalizedNameKey) {
        Optional<Classroom> byCode = classroomRepository.findFirstByClassCodeIgnoreCase(classCode);
        if (byCode.isPresent()) {
            return byCode;
        }
        List<Classroom> byName = classroomRepository.findByNormalizedClassName(normalizedNameKey);
        return byName.isEmpty() ? Optional.empty() : Optional.of(byName.getFirst());
    }

    private TuitionPackage requireEightSessionPackage(Long tuitionPackageId) {
        if (tuitionPackageId == null) {
            throw new BusinessException("tuitionPackageId is required");
        }
        TuitionPackage tuitionPackage = tuitionPackageRepository.findById(tuitionPackageId)
                .orElseThrow(() -> new NotFoundException("Tuition package not found"));
        if (tuitionPackage.getStatus() != TuitionPackageStatus.ACTIVE) {
            throw new BusinessException("Tuition package must be active");
        }
        if (tuitionPackage.getTotalSessions() == null
                || tuitionPackage.getTotalSessions() != REQUIRED_PACKAGE_SESSIONS) {
            throw new BusinessException("Selected package must have totalSessions = 8");
        }
        return tuitionPackage;
    }

    private LegacyImportPreviewResponse toPreviewResponse(TuitionPackage tuitionPackage, Analysis analysis) {
        return new LegacyImportPreviewResponse(
                tuitionPackage.getId(),
                tuitionPackage.getName(),
                analysis.sheets().size(),
                analysis.allRows().size(),
                analysis.validRows(),
                analysis.warningRows(),
                analysis.invalidRows(),
                analysis.newClassrooms(),
                analysis.existingClassrooms(),
                analysis.newStudents(),
                analysis.existingStudents(),
                analysis.newEnrollments(),
                analysis.duplicateEnrollments(),
                analysis.canConfirm(),
                CONFIRMATION_WARNING,
                analysis.sheets(),
                analysis.allRows()
        );
    }

    private LocalDate medianDate(List<LocalDate> dates) {
        if (dates == null || dates.isEmpty()) {
            return null;
        }
        return dates.get(dates.size() / 2);
    }

    private record Analysis(
            List<LegacyImportSheetPreview> sheets,
            List<LegacyImportRowPreview> allRows,
            int newClassrooms,
            int existingClassrooms,
            int newStudents,
            int existingStudents,
            int newEnrollments,
            int duplicateEnrollments,
            int validRows,
            int warningRows,
            int invalidRows,
            boolean canConfirm
    ) {
    }

    private record SessionCandidate(Long sessionId, LocalDate date) {
    }

    private record SessionPlan(
            List<SessionCandidate> candidates,
            int existingSessionCount,
            int sessionsToCreate,
            LocalDate firstSessionDate,
            LocalDate lastSessionDate
    ) {
    }
}
