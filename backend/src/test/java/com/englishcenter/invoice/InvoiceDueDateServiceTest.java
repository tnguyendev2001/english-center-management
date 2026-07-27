package com.englishcenter.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.englishcenter.settings.SystemSetting;
import com.englishcenter.settings.SystemSettingRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceDueDateServiceTest {
    @Mock
    private SystemSettingRepository systemSettingRepository;

    private InvoiceDueDateService service;

    @BeforeEach
    void setUp() {
        service = new InvoiceDueDateService(systemSettingRepository);
    }

    @Test
    void usesConfiguredDueDays() {
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey(InvoiceDueDateService.SETTING_KEY);
        setting.setSettingValue("10");
        when(systemSettingRepository.findBySettingKey(InvoiceDueDateService.SETTING_KEY))
                .thenReturn(Optional.of(setting));

        assertThat(service.calculateDueDate(LocalDate.of(2026, 8, 1)))
                .isEqualTo(LocalDate.of(2026, 8, 11));
    }

    @Test
    void defaultsToSevenDays() {
        when(systemSettingRepository.findBySettingKey(InvoiceDueDateService.SETTING_KEY))
                .thenReturn(Optional.empty());

        assertThat(service.calculateDueDate(LocalDate.of(2026, 8, 1)))
                .isEqualTo(LocalDate.of(2026, 8, 8));
    }
}
