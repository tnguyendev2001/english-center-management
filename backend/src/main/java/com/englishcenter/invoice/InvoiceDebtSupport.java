package com.englishcenter.invoice;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

/**
 * Authoritative collectible / outstanding debt rules for invoices.
 * Live debt includes only UNPAID and PARTIALLY_PAID invoices with remainingAmount &gt; 0.
 * CANCELED, PAID, and REPLACED invoices are never outstanding.
 */
public final class InvoiceDebtSupport {
    public static final Set<InvoiceStatus> COLLECTIBLE_STATUSES = EnumSet.of(
            InvoiceStatus.UNPAID,
            InvoiceStatus.PARTIALLY_PAID
    );

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private InvoiceDebtSupport() {
    }

    public static boolean isCollectibleStatus(InvoiceStatus status) {
        return status != null && COLLECTIBLE_STATUSES.contains(status);
    }

    public static boolean isCollectible(Invoice invoice) {
        return invoice != null && isCollectibleStatus(invoice.getStatus());
    }

    public static boolean isOutstanding(Invoice invoice) {
        if (!isCollectible(invoice)) {
            return false;
        }
        BigDecimal remaining = invoice.getRemainingAmount();
        return remaining != null && remaining.compareTo(ZERO) > 0;
    }
}
