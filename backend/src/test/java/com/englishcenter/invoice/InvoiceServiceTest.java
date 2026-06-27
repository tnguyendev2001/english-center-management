package com.englishcenter.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.englishcenter.invoice.mapper.InvoiceMapper;
import com.englishcenter.payment.PaymentRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {
    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Test
    void recalculateSetsPartiallyPaidFromValidPayments() {
        Invoice invoice = invoice(new BigDecimal("500000"));
        InvoiceService invoiceService = new InvoiceService(invoiceRepository, paymentRepository, new InvoiceMapper());

        when(paymentRepository.sumValidAmountByInvoiceId(1L)).thenReturn(new BigDecimal("200000"));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        Invoice recalculated = invoiceService.recalculateAndSave(invoice);

        assertThat(recalculated.getPaidAmount()).isEqualByComparingTo("200000");
        assertThat(recalculated.getRemainingAmount()).isEqualByComparingTo("300000");
        assertThat(recalculated.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
    }

    @Test
    void recalculateSetsPaidWhenValidPaymentsEqualFinalAmount() {
        Invoice invoice = invoice(new BigDecimal("500000"));
        InvoiceService invoiceService = new InvoiceService(invoiceRepository, paymentRepository, new InvoiceMapper());

        when(paymentRepository.sumValidAmountByInvoiceId(1L)).thenReturn(new BigDecimal("500000"));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        Invoice recalculated = invoiceService.recalculateAndSave(invoice);

        assertThat(recalculated.getPaidAmount()).isEqualByComparingTo("500000");
        assertThat(recalculated.getRemainingAmount()).isEqualByComparingTo("0");
        assertThat(recalculated.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void recalculateSetsUnpaidWhenNoValidPaymentsRemain() {
        Invoice invoice = invoice(new BigDecimal("500000"));
        invoice.setPaidAmount(new BigDecimal("200000"));
        invoice.setRemainingAmount(new BigDecimal("300000"));
        invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        InvoiceService invoiceService = new InvoiceService(invoiceRepository, paymentRepository, new InvoiceMapper());

        when(paymentRepository.sumValidAmountByInvoiceId(1L)).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        Invoice recalculated = invoiceService.recalculateAndSave(invoice);

        assertThat(recalculated.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(recalculated.getRemainingAmount()).isEqualByComparingTo("500000");
        assertThat(recalculated.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
    }

    private Invoice invoice(BigDecimal finalAmount) {
        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setFinalAmount(finalAmount);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setRemainingAmount(finalAmount);
        invoice.setStatus(InvoiceStatus.UNPAID);
        return invoice;
    }
}
