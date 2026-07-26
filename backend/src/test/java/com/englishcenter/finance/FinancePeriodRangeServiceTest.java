package com.englishcenter.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.englishcenter.common.config.AppTimeProperties;
import com.englishcenter.common.config.FinanceProperties;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.finance.dto.FinancePeriodRange;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.settings.SystemSettingRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinancePeriodRangeServiceTest {
    private static final ZoneId HCM = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private SystemSettingRepository systemSettingRepository;
    @Mock
    private CashTransactionRepository cashTransactionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private FinancialPeriodRepository financialPeriodRepository;

    private FinancePeriodRangeService service;

    @BeforeEach
    void setUp() {
        FinanceProperties financeProperties = new FinanceProperties();
        financeProperties.setFinanceStartDate(LocalDate.of(2026, 5, 1));
        AppTimeProperties timeProperties = new AppTimeProperties();
        timeProperties.setZoneId("Asia/Ho_Chi_Minh");
        Clock clock = Clock.fixed(
                LocalDate.of(2026, 7, 27).atStartOfDay(HCM).toInstant(),
                HCM
        );
        when(systemSettingRepository.findBySettingKey(any())).thenReturn(Optional.empty());
        service = new FinancePeriodRangeService(
                systemSettingRepository,
                financeProperties,
                timeProperties,
                cashTransactionRepository,
                paymentRepository,
                financialPeriodRepository,
                clock
        );
    }

    @Test
    void maxSelectableMonthIncludesLatestPaymentBeyondBusinessMonth() {
        when(cashTransactionRepository.findLatestPostedTransactionDate()).thenReturn(null);
        when(paymentRepository.findLatestValidPaymentDate()).thenReturn(LocalDate.of(2026, 8, 10));
        when(financialPeriodRepository.findFirstByOrderByYearDescMonthDesc()).thenReturn(Optional.empty());

        FinancePeriodRange range = service.getFinancePeriodRange();

        assertThat(range.businessDate()).isEqualTo(LocalDate.of(2026, 7, 27));
        assertThat(range.minSelectableMonth()).isEqualTo(YearMonth.of(2026, 5));
        assertThat(range.maxSelectableMonth()).isEqualTo(YearMonth.of(2026, 8));
        assertThat(range.hasDataBeyondBusinessMonth()).isTrue();
    }

    @Test
    void augustWithExistingDataIsSelectable() {
        when(cashTransactionRepository.findLatestPostedTransactionDate())
                .thenReturn(LocalDate.of(2026, 8, 10));
        when(paymentRepository.findLatestValidPaymentDate()).thenReturn(LocalDate.of(2026, 8, 10));
        when(financialPeriodRepository.findFirstByOrderByYearDescMonthDesc()).thenReturn(Optional.empty());

        service.validateMonthSelectable(2026, 8);
        assertThat(service.resolvePeriodEnd(2026, 8)).isEqualTo(LocalDate.of(2026, 8, 10));
    }

    @Test
    void trueFutureMonthWithoutDataIsRejected() {
        when(cashTransactionRepository.findLatestPostedTransactionDate()).thenReturn(null);
        when(paymentRepository.findLatestValidPaymentDate()).thenReturn(null);
        when(financialPeriodRepository.findFirstByOrderByYearDescMonthDesc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateMonthSelectable(2026, 8))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không thể xem kỳ tài chính trong tương lai.");
    }

    @Test
    void allScopeEndIncludesLatestFinancialData() {
        when(cashTransactionRepository.findLatestPostedTransactionDate())
                .thenReturn(LocalDate.of(2026, 8, 10));
        when(paymentRepository.findLatestValidPaymentDate()).thenReturn(LocalDate.of(2026, 8, 5));
        when(financialPeriodRepository.findFirstByOrderByYearDescMonthDesc()).thenReturn(Optional.empty());

        assertThat(service.resolveAllScopeEndDate()).isEqualTo(LocalDate.of(2026, 8, 10));
    }

    @Test
    void rejectsPaymentAfterBusinessDate() {
        assertThatThrownBy(() -> service.validatePaymentNotAfterBusinessDate(LocalDate.of(2026, 8, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ngày thanh toán không được lớn hơn ngày hiện tại.");

        service.validatePaymentNotAfterBusinessDate(LocalDate.of(2026, 7, 27));
    }
}
