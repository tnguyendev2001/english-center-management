package com.englishcenter.financial;

import static org.assertj.core.api.Assertions.assertThat;

import com.englishcenter.classroom.Classroom;
import com.englishcenter.debt.dto.StudentDebtSummaryResponse;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.invoice.dto.StudentTuitionSummaryResponse;
import com.englishcenter.payment.Payment;
import com.englishcenter.payment.PaymentMethod;
import com.englishcenter.payment.PaymentStatus;
import com.englishcenter.payment.dto.StudentPaymentSummaryResponse;
import com.englishcenter.student.Student;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class StudentFinancialSummaryAggregatorTest {
    @Test
    void aggregateTuitionSummariesGroupsInvoicesByStudentAndClassroom() {
        Student student = student(1L, "Nguyen Van A");
        Classroom classroom = classroom(10L, "Lop A1");

        List<Invoice> invoices = List.of(
                invoice(1L, student, classroom, new BigDecimal("500000"), new BigDecimal("0"), InvoiceStatus.UNPAID),
                invoice(2L, student, classroom, new BigDecimal("500000"), new BigDecimal("0"), InvoiceStatus.UNPAID)
        );

        List<StudentTuitionSummaryResponse> summaries = StudentFinancialSummaryAggregator.aggregateTuitionSummaries(invoices);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().totalTuitionAmount()).isEqualByComparingTo("1000000");
        assertThat(summaries.getFirst().totalPaidAmount()).isEqualByComparingTo("0");
        assertThat(summaries.getFirst().remainingDebt()).isEqualByComparingTo("1000000");
        assertThat(summaries.getFirst().totalInvoiceCount()).isEqualTo(2);
        assertThat(summaries.getFirst().unpaidCount()).isEqualTo(2);
    }

    @Test
    void aggregateTuitionSummariesExcludesCanceledAndReplacedFromDebt() {
        Student student = student(1L, "Nguyen Van A");
        Classroom classroom = classroom(10L, "Lop A1");

        List<Invoice> invoices = List.of(
                invoice(1L, student, classroom, new BigDecimal("500000"), new BigDecimal("300000"), InvoiceStatus.PARTIALLY_PAID),
                invoice(2L, student, classroom, new BigDecimal("500000"), new BigDecimal("500000"), InvoiceStatus.PAID),
                invoice(3L, student, classroom, new BigDecimal("400000"), new BigDecimal("0"), InvoiceStatus.REPLACED),
                invoice(4L, student, classroom, new BigDecimal("100000"), new BigDecimal("0"), InvoiceStatus.CANCELED)
        );

        List<StudentTuitionSummaryResponse> summaries = StudentFinancialSummaryAggregator.aggregateTuitionSummaries(invoices);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().totalTuitionAmount()).isEqualByComparingTo("1400000");
        assertThat(summaries.getFirst().totalPaidAmount()).isEqualByComparingTo("800000");
        assertThat(summaries.getFirst().remainingDebt()).isEqualByComparingTo("200000");
        assertThat(summaries.getFirst().hasReplacedInvoices()).isTrue();
    }

    @Test
    void aggregateDebtSummariesOnlyIncludesCollectibleInvoices() {
        Student student = student(1L, "Nguyen Van A");
        Classroom classroom = classroom(10L, "Lop A1");

        Invoice unpaid = invoice(1L, student, classroom, new BigDecimal("500000"), new BigDecimal("0"), InvoiceStatus.UNPAID);
        unpaid.setRemainingAmount(new BigDecimal("500000"));
        unpaid.setDueDate(LocalDate.of(2026, 7, 20));

        Invoice partial = invoice(2L, student, classroom, new BigDecimal("500000"), new BigDecimal("200000"), InvoiceStatus.PARTIALLY_PAID);
        partial.setRemainingAmount(new BigDecimal("300000"));
        partial.setDueDate(LocalDate.of(2026, 7, 10));

        List<StudentDebtSummaryResponse> summaries = StudentFinancialSummaryAggregator.aggregateDebtSummaries(List.of(unpaid, partial));

        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().totalRemainingDebt()).isEqualByComparingTo("800000");
        assertThat(summaries.getFirst().debtInvoiceCount()).isEqualTo(2);
        assertThat(summaries.getFirst().nearestDueDate()).isEqualTo(LocalDate.of(2026, 7, 10));
    }

    @Test
    void aggregatePaymentSummariesCountsValidPaymentsOnly() {
        Student student = student(1L, "Nguyen Van A");
        Classroom classroom = classroom(10L, "Lop A1");

        List<Payment> payments = List.of(
                payment(1L, student, classroom, new BigDecimal("300000"), PaymentStatus.VALID, PaymentMethod.CASH, LocalDate.of(2026, 7, 1)),
                payment(2L, student, classroom, new BigDecimal("200000"), PaymentStatus.CANCELED, PaymentMethod.CASH, LocalDate.of(2026, 7, 2)),
                payment(3L, student, classroom, new BigDecimal("100000"), PaymentStatus.VALID, PaymentMethod.BANK_TRANSFER, LocalDate.of(2026, 7, 5))
        );

        List<StudentPaymentSummaryResponse> summaries = StudentFinancialSummaryAggregator.aggregatePaymentSummaries(
                payments,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        );

        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().totalPaidAmount()).isEqualByComparingTo("400000");
        assertThat(summaries.getFirst().paymentCount()).isEqualTo(2);
        assertThat(summaries.getFirst().lastPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
    }

    private Student student(Long id, String name) {
        Student student = new Student();
        student.setId(id);
        student.setStudentCode("ST" + String.format("%05d", id));
        student.setFullName(name);
        return student;
    }

    private Classroom classroom(Long id, String name) {
        Classroom classroom = new Classroom();
        classroom.setId(id);
        classroom.setClassName(name);
        return classroom;
    }

    private Invoice invoice(
            Long id,
            Student student,
            Classroom classroom,
            BigDecimal finalAmount,
            BigDecimal paidAmount,
            InvoiceStatus status
    ) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setStudent(student);
        invoice.setClassroom(classroom);
        invoice.setFinalAmount(finalAmount);
        invoice.setPaidAmount(paidAmount);
        invoice.setRemainingAmount(finalAmount.subtract(paidAmount));
        invoice.setStatus(status);
        invoice.setDueDate(LocalDate.of(2026, 7, 15));
        return invoice;
    }

    private Payment payment(
            Long id,
            Student student,
            Classroom classroom,
            BigDecimal amount,
            PaymentStatus status,
            PaymentMethod method,
            LocalDate paymentDate
    ) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setStudent(student);
        payment.setClassroom(classroom);
        payment.setAmount(amount);
        payment.setStatus(status);
        payment.setMethod(method);
        payment.setPaymentDate(paymentDate);
        payment.setCreatedAt(LocalDateTime.of(2026, 7, paymentDate.getDayOfMonth(), 10, 0));
        return payment;
    }
}
