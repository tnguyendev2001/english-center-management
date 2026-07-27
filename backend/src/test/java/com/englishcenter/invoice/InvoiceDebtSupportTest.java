package com.englishcenter.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InvoiceDebtSupportTest {
    @Test
    void isOutstandingRequiresCollectibleStatusAndPositiveRemaining() {
        Invoice unpaid = invoice(InvoiceStatus.UNPAID, new BigDecimal("100000"));
        Invoice zeroRemaining = invoice(InvoiceStatus.UNPAID, BigDecimal.ZERO);
        Invoice canceled = invoice(InvoiceStatus.CANCELED, new BigDecimal("100000"));
        Invoice paid = invoice(InvoiceStatus.PAID, BigDecimal.ZERO);
        Invoice replaced = invoice(InvoiceStatus.REPLACED, new BigDecimal("100000"));

        assertThat(InvoiceDebtSupport.isOutstanding(unpaid)).isTrue();
        assertThat(InvoiceDebtSupport.isOutstanding(zeroRemaining)).isFalse();
        assertThat(InvoiceDebtSupport.isOutstanding(canceled)).isFalse();
        assertThat(InvoiceDebtSupport.isOutstanding(paid)).isFalse();
        assertThat(InvoiceDebtSupport.isOutstanding(replaced)).isFalse();
    }

    @Test
    void isCollectibleMatchesUnpaidAndPartiallyPaidOnly() {
        assertThat(InvoiceDebtSupport.isCollectibleStatus(InvoiceStatus.UNPAID)).isTrue();
        assertThat(InvoiceDebtSupport.isCollectibleStatus(InvoiceStatus.PARTIALLY_PAID)).isTrue();
        assertThat(InvoiceDebtSupport.isCollectibleStatus(InvoiceStatus.CANCELED)).isFalse();
        assertThat(InvoiceDebtSupport.isCollectibleStatus(InvoiceStatus.PAID)).isFalse();
        assertThat(InvoiceDebtSupport.isCollectibleStatus(InvoiceStatus.REPLACED)).isFalse();
    }

    private Invoice invoice(InvoiceStatus status, BigDecimal remainingAmount) {
        Invoice invoice = new Invoice();
        invoice.setStatus(status);
        invoice.setRemainingAmount(remainingAmount);
        return invoice;
    }
}
