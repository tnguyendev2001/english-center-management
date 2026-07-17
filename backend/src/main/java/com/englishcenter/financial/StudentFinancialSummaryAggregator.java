package com.englishcenter.financial;

import com.englishcenter.debt.dto.StudentDebtSummaryResponse;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.invoice.dto.StudentTuitionSummaryResponse;
import com.englishcenter.payment.Payment;
import com.englishcenter.payment.PaymentStatus;
import com.englishcenter.payment.dto.StudentPaymentSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StudentFinancialSummaryAggregator {
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private StudentFinancialSummaryAggregator() {
    }

    public static List<StudentTuitionSummaryResponse> aggregateTuitionSummaries(List<Invoice> invoices) {
        Map<String, TuitionAccumulator> groups = new HashMap<>();

        for (Invoice invoice : invoices) {
            if (invoice.getStatus() == InvoiceStatus.CANCELED) {
                continue;
            }

            String key = groupKey(invoice.getStudent().getId(), invoice.getClassroom().getId());
            TuitionAccumulator accumulator = groups.computeIfAbsent(key, ignored -> new TuitionAccumulator(
                    invoice.getStudent().getId(),
                    invoice.getStudent().getStudentCode(),
                    invoice.getStudent().getFullName(),
                    invoice.getClassroom().getId(),
                    invoice.getClassroom().getClassName()
            ));

            accumulator.totalTuitionAmount = accumulator.totalTuitionAmount.add(invoice.getFinalAmount());
            accumulator.totalPaidAmount = accumulator.totalPaidAmount.add(invoice.getPaidAmount());
            accumulator.totalInvoiceCount++;

            switch (invoice.getStatus()) {
                case UNPAID -> {
                    accumulator.unpaidCount++;
                    accumulator.remainingDebt = accumulator.remainingDebt.add(invoice.getRemainingAmount());
                }
                case PARTIALLY_PAID -> {
                    accumulator.partialCount++;
                    accumulator.remainingDebt = accumulator.remainingDebt.add(invoice.getRemainingAmount());
                }
                case PAID -> accumulator.paidCount++;
                case REPLACED -> accumulator.hasReplacedInvoices = true;
                default -> {
                }
            }
        }

        return groups.values().stream()
                .map(TuitionAccumulator::toResponse)
                .sorted(Comparator.comparing(StudentTuitionSummaryResponse::studentName)
                        .thenComparing(StudentTuitionSummaryResponse::classroomName))
                .toList();
    }

    public static List<StudentDebtSummaryResponse> aggregateDebtSummaries(List<Invoice> debtInvoices) {
        Map<String, DebtAccumulator> groups = new HashMap<>();

        for (Invoice invoice : debtInvoices) {
            if (invoice.getStatus() != InvoiceStatus.UNPAID && invoice.getStatus() != InvoiceStatus.PARTIALLY_PAID) {
                continue;
            }

            if (invoice.getRemainingAmount().compareTo(ZERO) <= 0) {
                continue;
            }

            String key = groupKey(invoice.getStudent().getId(), invoice.getClassroom().getId());
            DebtAccumulator accumulator = groups.computeIfAbsent(key, ignored -> new DebtAccumulator(
                    invoice.getStudent().getId(),
                    invoice.getStudent().getStudentCode(),
                    invoice.getStudent().getFullName(),
                    invoice.getClassroom().getId(),
                    invoice.getClassroom().getClassName()
            ));

            accumulator.totalRemainingDebt = accumulator.totalRemainingDebt.add(invoice.getRemainingAmount());
            accumulator.debtInvoiceCount++;

            if (invoice.getStatus() == InvoiceStatus.UNPAID) {
                accumulator.unpaidCount++;
            } else {
                accumulator.partialCount++;
            }

            if (accumulator.nearestDueDate == null || invoice.getDueDate().isBefore(accumulator.nearestDueDate)) {
                accumulator.nearestDueDate = invoice.getDueDate();
            }
        }

        return groups.values().stream()
                .map(DebtAccumulator::toResponse)
                .sorted(Comparator.comparing(StudentDebtSummaryResponse::studentName)
                        .thenComparing(StudentDebtSummaryResponse::classroomName))
                .toList();
    }

    public static List<StudentPaymentSummaryResponse> aggregatePaymentSummaries(
            List<Payment> payments,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Map<String, PaymentAccumulator> groups = new HashMap<>();

        for (Payment payment : payments) {
            if (payment.getStatus() != PaymentStatus.VALID) {
                continue;
            }

            if (fromDate != null && payment.getPaymentDate().isBefore(fromDate)) {
                continue;
            }

            if (toDate != null && payment.getPaymentDate().isAfter(toDate)) {
                continue;
            }

            String key = groupKey(payment.getStudent().getId(), payment.getClassroom().getId());
            PaymentAccumulator accumulator = groups.computeIfAbsent(key, ignored -> new PaymentAccumulator(
                    payment.getStudent().getId(),
                    payment.getStudent().getStudentCode(),
                    payment.getStudent().getFullName(),
                    payment.getClassroom().getId(),
                    payment.getClassroom().getClassName()
            ));

            accumulator.totalPaidAmount = accumulator.totalPaidAmount.add(payment.getAmount());
            accumulator.paymentCount++;

            if (accumulator.lastPaymentDate == null
                    || payment.getPaymentDate().isAfter(accumulator.lastPaymentDate)
                    || (payment.getPaymentDate().equals(accumulator.lastPaymentDate)
                    && payment.getCreatedAt().isAfter(accumulator.lastCreatedAt))) {
                accumulator.lastPaymentDate = payment.getPaymentDate();
                accumulator.lastPaymentMethod = payment.getMethod();
                accumulator.lastCreatedAt = payment.getCreatedAt();
            }
        }

        return groups.values().stream()
                .map(PaymentAccumulator::toResponse)
                .sorted(Comparator.comparing(StudentPaymentSummaryResponse::studentName)
                        .thenComparing(StudentPaymentSummaryResponse::classroomName))
                .toList();
    }

    private static String groupKey(Long studentId, Long classroomId) {
        return studentId + ":" + classroomId;
    }

    private static final class TuitionAccumulator {
        private final Long studentId;
        private final String studentCode;
        private final String studentName;
        private final Long classroomId;
        private final String classroomName;
        private BigDecimal totalTuitionAmount = ZERO;
        private BigDecimal totalPaidAmount = ZERO;
        private BigDecimal remainingDebt = ZERO;
        private int totalInvoiceCount;
        private int unpaidCount;
        private int partialCount;
        private int paidCount;
        private boolean hasReplacedInvoices;

        private TuitionAccumulator(
                Long studentId,
                String studentCode,
                String studentName,
                Long classroomId,
                String classroomName
        ) {
            this.studentId = studentId;
            this.studentCode = studentCode;
            this.studentName = studentName;
            this.classroomId = classroomId;
            this.classroomName = classroomName;
        }

        private StudentTuitionSummaryResponse toResponse() {
            return new StudentTuitionSummaryResponse(
                    studentId,
                    studentCode,
                    studentName,
                    classroomId,
                    classroomName,
                    totalTuitionAmount,
                    totalPaidAmount,
                    remainingDebt,
                    totalInvoiceCount,
                    unpaidCount,
                    partialCount,
                    paidCount,
                    hasReplacedInvoices
            );
        }
    }

    private static final class DebtAccumulator {
        private final Long studentId;
        private final String studentCode;
        private final String studentName;
        private final Long classroomId;
        private final String classroomName;
        private BigDecimal totalRemainingDebt = ZERO;
        private int debtInvoiceCount;
        private int unpaidCount;
        private int partialCount;
        private LocalDate nearestDueDate;

        private DebtAccumulator(
                Long studentId,
                String studentCode,
                String studentName,
                Long classroomId,
                String classroomName
        ) {
            this.studentId = studentId;
            this.studentCode = studentCode;
            this.studentName = studentName;
            this.classroomId = classroomId;
            this.classroomName = classroomName;
        }

        private StudentDebtSummaryResponse toResponse() {
            return new StudentDebtSummaryResponse(
                    studentId,
                    studentCode,
                    studentName,
                    classroomId,
                    classroomName,
                    totalRemainingDebt,
                    debtInvoiceCount,
                    unpaidCount,
                    partialCount,
                    nearestDueDate
            );
        }
    }

    private static final class PaymentAccumulator {
        private final Long studentId;
        private final String studentCode;
        private final String studentName;
        private final Long classroomId;
        private final String classroomName;
        private BigDecimal totalPaidAmount = ZERO;
        private int paymentCount;
        private LocalDate lastPaymentDate;
        private com.englishcenter.payment.PaymentMethod lastPaymentMethod;
        private java.time.LocalDateTime lastCreatedAt = java.time.LocalDateTime.MIN;

        private PaymentAccumulator(
                Long studentId,
                String studentCode,
                String studentName,
                Long classroomId,
                String classroomName
        ) {
            this.studentId = studentId;
            this.studentCode = studentCode;
            this.studentName = studentName;
            this.classroomId = classroomId;
            this.classroomName = classroomName;
        }

        private StudentPaymentSummaryResponse toResponse() {
            return new StudentPaymentSummaryResponse(
                    studentId,
                    studentCode,
                    studentName,
                    classroomId,
                    classroomName,
                    totalPaidAmount,
                    paymentCount,
                    lastPaymentDate,
                    lastPaymentMethod
            );
        }
    }
}
