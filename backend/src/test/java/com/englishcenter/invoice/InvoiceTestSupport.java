package com.englishcenter.invoice;

import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.common.config.AppTimeProperties;
import com.englishcenter.invoice.mapper.InvoiceMapper;
import com.englishcenter.payment.mapper.PaymentMapper;
import com.englishcenter.settings.SystemSettingRepository;
import org.mockito.Mockito;

/**
 * Shared test doubles for billing mappers/services that gained constructor dependencies.
 */
public final class InvoiceTestSupport {
    private InvoiceTestSupport() {
    }

    public static InvoiceMapper invoiceMapper() {
        return new InvoiceMapper(
                new BillingCycleLabelService(),
                new InvoiceEffectivePeriodService(Mockito.mock(ClassSessionRepository.class)),
                new AppTimeProperties()
        );
    }

    public static PaymentMapper paymentMapper() {
        return new PaymentMapper(new BillingCycleLabelService());
    }

    public static InvoiceBillingSnapshotService billingSnapshotService() {
        SystemSettingRepository settings = Mockito.mock(SystemSettingRepository.class);
        Mockito.lenient()
                .when(settings.findBySettingKey(InvoiceDueDateService.SETTING_KEY))
                .thenReturn(java.util.Optional.empty());
        return new InvoiceBillingSnapshotService(new InvoiceDueDateService(settings));
    }
}
