package com.englishcenter.importdata;

import com.englishcenter.attendance.AttendanceService;
import com.englishcenter.classpackage.ClassPackage;
import com.englishcenter.classpackage.ClassPackageRepository;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRenewalService;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classroom.ClassroomStatus;
import com.englishcenter.classroom.dto.PackageRenewalCommand;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionService;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.classsession.dto.GenerateClassSessionsResponse;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentService;
import com.englishcenter.enrollment.EnrollmentSessionService;
import com.englishcenter.importdata.dto.LegacyImportRowPreview;
import com.englishcenter.importdata.dto.LegacyImportSheetPreview;
import com.englishcenter.student.Student;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.student.StudentService;
import com.englishcenter.student.StudentStatus;
import com.englishcenter.studentpackage.StudentPackageRepository;
import com.englishcenter.studentpackage.StudentPackageSourceType;
import com.englishcenter.tuitionpackage.TuitionPackage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side of legacy import with explicit transaction boundaries.
 * Kept separate from {@link LegacyStudentImportService} to avoid self-invocation issues.
 */
@Service
public class LegacyStudentImportWriter {
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(17, 0);
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(18, 30);

    private final ClassroomRepository classroomRepository;
    private final ClassPackageRepository classPackageRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassSessionRepository classSessionRepository;
    private final StudentPackageRepository studentPackageRepository;
    private final ClassSessionService classSessionService;
    private final EnrollmentService enrollmentService;
    private final ClassroomRenewalService classroomRenewalService;
    private final AttendanceService attendanceService;
    private final EnrollmentSessionService enrollmentSessionService;

    public LegacyStudentImportWriter(
            ClassroomRepository classroomRepository,
            ClassPackageRepository classPackageRepository,
            StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository,
            ClassSessionRepository classSessionRepository,
            StudentPackageRepository studentPackageRepository,
            ClassSessionService classSessionService,
            EnrollmentService enrollmentService,
            ClassroomRenewalService classroomRenewalService,
            AttendanceService attendanceService,
            EnrollmentSessionService enrollmentSessionService
    ) {
        this.classroomRepository = classroomRepository;
        this.classPackageRepository = classPackageRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.classSessionRepository = classSessionRepository;
        this.studentPackageRepository = studentPackageRepository;
        this.classSessionService = classSessionService;
        this.enrollmentService = enrollmentService;
        this.classroomRenewalService = classroomRenewalService;
        this.attendanceService = attendanceService;
        this.enrollmentSessionService = enrollmentSessionService;
    }

    @Transactional
    public ClassroomEnsureResult ensureClassroom(
            LegacyImportSheetPreview sheet,
            TuitionPackage tuitionPackage,
            Map<String, Classroom> classroomByKey
    ) {
        String key = LegacyImportNormalizer.normalizeClassroomNameKey(sheet.sheetName());
        if (classroomByKey.containsKey(key)) {
            Classroom existing = classroomByKey.get(key);
            ensureClassPackageLinked(existing, tuitionPackage);
            return new ClassroomEnsureResult(existing, false);
        }

        Optional<Classroom> existing = findExistingClassroom(sheet.proposedClassCode(), key);
        if (existing.isPresent()) {
            classroomByKey.put(key, existing.get());
            ensureClassPackageLinked(existing.get(), tuitionPackage);
            return new ClassroomEnsureResult(existing.get(), false);
        }

        Classroom classroom = new Classroom();
        classroom.setClassCode(uniqueClassCode(sheet.proposedClassCode()));
        classroom.setClassName(sheet.normalizedClassroomName());
        classroom.setLevel(deriveLevel(sheet.sheetName()));
        classroom.setTeacherName("Chưa cập nhật");
        classroom.setStartDate(sheet.classroomStartDate());
        classroom.setDaysOfWeek(new HashSet<>(sheet.daysOfWeek()));
        classroom.setStartTime(DEFAULT_START_TIME);
        classroom.setEndTime(DEFAULT_END_TIME);
        classroom.setStatus(ClassroomStatus.ONGOING);
        classroom.setNote("Tạo từ nhập liệu legacy Excel");
        classroom = classroomRepository.save(classroom);
        classroomByKey.put(key, classroom);
        ensureClassPackageLinked(classroom, tuitionPackage);
        return new ClassroomEnsureResult(classroom, true);
    }

    @Transactional
    public GenerateClassSessionsResponse generateSessions(Classroom classroom) {
        LocalDate today = LocalDate.now();
        LocalDate from = classroom.getStartDate();
        LocalDate to = today.isBefore(from) ? from : today;
        return classSessionService.generateUpToDate(classroom.getId(), from, to);
    }

    /**
     * Atomic per-student import: enroll + renew cycles + attendance + progress sync.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StudentImportResult importStudent(
            Classroom classroom,
            TuitionPackage tuitionPackage,
            LegacyImportRowPreview row,
            Map<String, Student> studentsByPhone
    ) {
        StudentResolveResult studentResult = resolveOrCreateStudent(row, studentsByPhone);

        if (enrollmentRepository.existsByStudentIdAndClassroomId(studentResult.student().getId(), classroom.getId())) {
            return new StudentImportResult(
                    studentResult.student(),
                    studentResult.created(),
                    true,
                    0,
                    0,
                    0
            );
        }

        LocalDate today = LocalDate.now();
        List<ClassSession> eligibleSessions = classSessionRepository
                .findByClassroomIdAndSessionDateBetweenAndStatusNotOrderBySessionDateAscStartTimeAsc(
                        classroom.getId(),
                        row.learningStartDate(),
                        today,
                        ClassSessionStatus.CANCELED
                );
        List<LocalDate> eligibleDates = eligibleSessions.stream().map(ClassSession::getSessionDate).toList();
        int packageCycles = LegacyImportCycleCalculator.packageCycles(eligibleSessions.size());

        Enrollment enrollment = enrollmentService.enrollFromLegacyImport(
                studentResult.student(),
                classroom,
                tuitionPackage,
                row.learningStartDate()
        );

        int invoicesCreated = 1;
        int packageCyclesCreated = 1;
        int currentMaxCycle = studentPackageRepository.findMaxCycleNoByEnrollmentId(enrollment.getId());

        for (int cycle = currentMaxCycle + 1; cycle <= packageCycles; cycle++) {
            LocalDate effectiveDate = LegacyImportCycleCalculator.cycleEffectiveDate(
                    row.learningStartDate(),
                    eligibleDates,
                    cycle
            );
            classroomRenewalService.renewPackage(new PackageRenewalCommand(
                    enrollment.getId(),
                    tuitionPackage.getId(),
                    effectiveDate,
                    StudentPackageSourceType.LEGACY_IMPORT,
                    "Nhập liệu legacy Excel",
                    true
            ));
            invoicesCreated++;
            packageCyclesCreated++;
        }

        List<Long> sessionIds = eligibleSessions.stream().map(ClassSession::getId).toList();
        int attendancesCreated = attendanceService.markLegacyAttendancePresent(enrollment.getId(), sessionIds);
        enrollmentSessionService.recalculateEnrollmentProgress(enrollment.getId());

        return new StudentImportResult(
                studentResult.student(),
                studentResult.created(),
                false,
                attendancesCreated,
                invoicesCreated,
                packageCyclesCreated
        );
    }

    private StudentResolveResult resolveOrCreateStudent(
            LegacyImportRowPreview row,
            Map<String, Student> studentsByPhone
    ) {
        if (row.phone() != null && studentsByPhone.containsKey(row.phone())) {
            return new StudentResolveResult(studentsByPhone.get(row.phone()), false);
        }
        if (row.existingStudentId() != null) {
            Student student = studentRepository.findById(row.existingStudentId()).orElseThrow();
            if (row.phone() != null) {
                studentsByPhone.put(row.phone(), student);
            }
            return new StudentResolveResult(student, false);
        }
        if (row.phone() != null) {
            Optional<Student> byPhone = studentRepository.findFirstByNormalizedPhone(row.phone());
            if (byPhone.isPresent()) {
                studentsByPhone.put(row.phone(), byPhone.get());
                return new StudentResolveResult(byPhone.get(), false);
            }
        }

        Student student = new Student();
        student.setFullName(row.studentName());
        student.setPhone(row.phone());
        student.setStatus(StudentStatus.ACTIVE);
        student.setNote("Tạo từ nhập liệu legacy Excel");
        student = studentRepository.saveAndFlush(student);
        student.setStudentCode(StudentService.formatStudentCode(student.getId()));
        student = studentRepository.save(student);
        if (row.phone() != null) {
            studentsByPhone.put(row.phone(), student);
        }
        return new StudentResolveResult(student, true);
    }

    private void ensureClassPackageLinked(Classroom classroom, TuitionPackage tuitionPackage) {
        ClassPackage classPackage = classPackageRepository
                .findByClassroomIdAndTuitionPackageId(classroom.getId(), tuitionPackage.getId())
                .orElse(null);
        if (classPackage == null) {
            classPackage = new ClassPackage();
            classPackage.setClassroom(classroom);
            classPackage.setTuitionPackage(tuitionPackage);
        }
        classPackage.setActive(true);
        classPackageRepository.save(classPackage);
    }

    private Optional<Classroom> findExistingClassroom(String classCode, String normalizedNameKey) {
        Optional<Classroom> byCode = classroomRepository.findFirstByClassCodeIgnoreCase(classCode);
        if (byCode.isPresent()) {
            return byCode;
        }
        List<Classroom> byName = classroomRepository.findByNormalizedClassName(normalizedNameKey);
        return byName.isEmpty() ? Optional.empty() : Optional.of(byName.getFirst());
    }

    private String uniqueClassCode(String base) {
        String candidate = base;
        int suffix = 1;
        while (classroomRepository.existsByClassCode(candidate)) {
            String suffixText = "-" + suffix;
            int maxBase = 50 - suffixText.length();
            candidate = (base.length() <= maxBase ? base : base.substring(0, maxBase)) + suffixText;
            suffix++;
        }
        return candidate;
    }

    private String deriveLevel(String sheetName) {
        String name = sheetName == null ? "" : sheetName.toUpperCase();
        if (name.contains("GRADE")) {
            int idx = name.indexOf("GRADE");
            String part = name.substring(idx).trim();
            return part.length() > 100 ? part.substring(0, 100) : part;
        }
        return "Imported";
    }

    public record ClassroomEnsureResult(Classroom classroom, boolean created) {
    }

    public record StudentImportResult(
            Student student,
            boolean studentCreated,
            boolean enrollmentSkipped,
            int attendancesCreated,
            int invoicesCreated,
            int packageCyclesCreated
    ) {
    }

    private record StudentResolveResult(Student student, boolean created) {
    }
}
