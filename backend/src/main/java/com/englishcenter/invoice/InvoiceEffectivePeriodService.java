package com.englishcenter.invoice;

import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.studentpackage.StudentPackage;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derives package-cycle effective dates. estimatedEffectiveTo is only set when the
 * corresponding non-canceled ClassSession already exists — never invented from calendar months.
 */
@Service
public class InvoiceEffectivePeriodService {
    private final ClassSessionRepository classSessionRepository;

    public InvoiceEffectivePeriodService(ClassSessionRepository classSessionRepository) {
        this.classSessionRepository = classSessionRepository;
    }

    public LocalDate resolveEffectiveFrom(Invoice invoice) {
        if (invoice.getEffectiveFrom() != null) {
            return invoice.getEffectiveFrom();
        }
        StudentPackage studentPackage = invoice.getStudentPackage();
        if (studentPackage != null && studentPackage.getStartDate() != null) {
            return studentPackage.getStartDate();
        }
        Enrollment enrollment = invoice.getEnrollment();
        return enrollment != null ? enrollment.getStartDate() : null;
    }

    /**
     * For cycle N with S sessions, covers enrollment sessions ((N-1)*S + 1) .. (N*S)
     * counted from non-canceled classroom sessions on/after enrollment learning start.
     */
    @Transactional(readOnly = true)
    public LocalDate estimateEffectiveTo(Invoice invoice) {
        Integer cycleNo = invoice.getCycleNo();
        if (cycleNo == null && invoice.getStudentPackage() != null) {
            cycleNo = invoice.getStudentPackage().getCycleNo();
        }
        Integer sessionCount = invoice.getTotalSessionsSnapshot();
        if (cycleNo == null || cycleNo < 1 || sessionCount == null || sessionCount < 1) {
            return null;
        }
        if (invoice.getClassroom() == null || invoice.getEnrollment() == null) {
            return null;
        }

        LocalDate learningStart = invoice.getEnrollment().getStartDate();
        if (learningStart == null) {
            return null;
        }

        List<ClassSession> sessions = classSessionRepository
                .findByClassroomIdAndSessionDateBetweenAndStatusNotOrderBySessionDateAscStartTimeAsc(
                        invoice.getClassroom().getId(),
                        learningStart,
                        LocalDate.of(9999, 12, 31),
                        ClassSessionStatus.CANCELED
                );

        int endIndex = cycleNo * sessionCount - 1;
        if (sessions.size() <= endIndex) {
            return null;
        }
        return sessions.get(endIndex).getSessionDate();
    }
}
