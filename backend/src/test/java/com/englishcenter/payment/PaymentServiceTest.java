package com.englishcenter.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishcenter.classroom.Classroom;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceService;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.payment.dto.CancelPaymentRequest;
import com.englishcenter.payment.dto.CreatePaymentRequest;
import com.englishcenter.payment.dto.PaymentResponse;
import com.englishcenter.payment.mapper.PaymentMapper;
import com.englishcenter.student.Student;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceService invoiceService;

    private final PaymentMapper paymentMapper = new PaymentMapper();

    @Test
    void createPaymentSavesValidPaymentAndRecalculatesInvoice() {
        PaymentService paymentService = newService();
        Invoice invoice = invoice(InvoiceStatus.UNPAID, new BigDecimal("500000"));
        CreatePaymentRequest request = createRequest(new BigDecimal("200000"));

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceService.recalculateAndSave(invoice)).thenReturn(invoice);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(10L);
            payment.setCreatedAt(LocalDateTime.now());
            return payment;
        });

        PaymentResponse response = paymentService.createPayment(1L, request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.amount()).isEqualByComparingTo("200000");
        assertThat(response.status()).isEqualTo(PaymentStatus.VALID);
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceService, times(2)).recalculateAndSave(invoice);
    }

    @Test
    void createPaymentRejectsOverpayment() {
        PaymentService paymentService = newService();
        Invoice invoice = invoice(InvoiceStatus.UNPAID, new BigDecimal("100000"));

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceService.recalculateAndSave(invoice)).thenReturn(invoice);

        assertThatThrownBy(() -> paymentService.createPayment(1L, createRequest(new BigDecimal("200000"))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Payment amount must not exceed invoice remaining amount");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createPaymentRejectsCanceledInvoice() {
        PaymentService paymentService = newService();
        Invoice invoice = invoice(InvoiceStatus.CANCELED, new BigDecimal("500000"));

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceService.recalculateAndSave(invoice)).thenReturn(invoice);

        assertThatThrownBy(() -> paymentService.createPayment(1L, createRequest(new BigDecimal("200000"))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot create payment for canceled invoice");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createPaymentRejectsPaidInvoice() {
        PaymentService paymentService = newService();
        Invoice invoice = invoice(InvoiceStatus.PAID, BigDecimal.ZERO);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceService.recalculateAndSave(invoice)).thenReturn(invoice);

        assertThatThrownBy(() -> paymentService.createPayment(1L, createRequest(new BigDecimal("200000"))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot create payment for paid invoice");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void cancelPaymentStoresReasonAndRecalculatesInvoice() {
        PaymentService paymentService = newService();
        Invoice invoice = invoice(InvoiceStatus.PARTIALLY_PAID, new BigDecimal("300000"));
        Payment payment = payment(invoice);

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        PaymentResponse response = paymentService.cancelPayment(10L, new CancelPaymentRequest("Wrong amount"));

        assertThat(response.status()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(response.cancelReason()).isEqualTo("Wrong amount");
        assertThat(response.canceledAt()).isNotNull();
        verify(paymentRepository).save(payment);
        verify(invoiceService).recalculateAndSave(invoice);
        verify(paymentRepository, never()).delete(any(Payment.class));
    }

    private PaymentService newService() {
        return new PaymentService(paymentRepository, invoiceRepository, invoiceService, paymentMapper);
    }

    private CreatePaymentRequest createRequest(BigDecimal amount) {
        return new CreatePaymentRequest(
                amount,
                LocalDate.of(2026, 7, 1),
                PaymentMethod.CASH,
                "Cash payment"
        );
    }

    private Invoice invoice(InvoiceStatus status, BigDecimal remainingAmount) {
        Student student = new Student();
        student.setId(1L);
        student.setFullName("Nguyen Van A");

        Classroom classroom = new Classroom();
        classroom.setId(2L);
        classroom.setClassName("Starter A");

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceCode("INV001");
        invoice.setStudent(student);
        invoice.setClassroom(classroom);
        invoice.setFinalAmount(new BigDecimal("500000"));
        invoice.setPaidAmount(invoice.getFinalAmount().subtract(remainingAmount));
        invoice.setRemainingAmount(remainingAmount);
        invoice.setStatus(status);
        return invoice;
    }

    private Payment payment(Invoice invoice) {
        Payment payment = new Payment();
        payment.setId(10L);
        payment.setPaymentCode("PAY001");
        payment.setInvoice(invoice);
        payment.setStudent(invoice.getStudent());
        payment.setClassroom(invoice.getClassroom());
        payment.setAmount(new BigDecimal("200000"));
        payment.setPaymentDate(LocalDate.of(2026, 7, 1));
        payment.setMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.VALID);
        payment.setCreatedAt(LocalDateTime.now());
        return payment;
    }
}
