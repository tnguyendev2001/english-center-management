package com.englishcenter.tuitionpackage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.tuitionpackage.dto.TuitionPackageCreateRequest;
import com.englishcenter.tuitionpackage.dto.TuitionPackageResponse;
import com.englishcenter.tuitionpackage.mapper.TuitionPackageMapper;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TuitionPackageServiceTest {
    @Mock
    private TuitionPackageRepository tuitionPackageRepository;

    private final TuitionPackageMapper tuitionPackageMapper = new TuitionPackageMapper();

    @Test
    void createAcceptsValidPrice() {
        TuitionPackageService tuitionPackageService = new TuitionPackageService(
                tuitionPackageRepository,
                tuitionPackageMapper
        );
        TuitionPackageCreateRequest request = validCreateRequest(new BigDecimal("500000"));

        when(tuitionPackageRepository.save(any(TuitionPackage.class))).thenAnswer(invocation -> {
            TuitionPackage tuitionPackage = invocation.getArgument(0);
            tuitionPackage.setId(1L);
            return tuitionPackage;
        });

        TuitionPackageResponse response = tuitionPackageService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("8 sessions");
        assertThat(response.price()).isEqualByComparingTo("500000");
        assertThat(response.status()).isEqualTo(TuitionPackageStatus.ACTIVE);
        verify(tuitionPackageRepository).save(any(TuitionPackage.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void createRejectsPriceLessThanOrEqualToZero(String price) {
        TuitionPackageService tuitionPackageService = new TuitionPackageService(
                tuitionPackageRepository,
                tuitionPackageMapper
        );
        TuitionPackageCreateRequest request = validCreateRequest(new BigDecimal(price));

        assertThatThrownBy(() -> tuitionPackageService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Price must be greater than 0");

        verifyNoInteractions(tuitionPackageRepository);
    }

    @Test
    void deactivateSetsStatusInactive() {
        TuitionPackageService tuitionPackageService = new TuitionPackageService(
                tuitionPackageRepository,
                tuitionPackageMapper
        );
        TuitionPackage tuitionPackage = new TuitionPackage();
        tuitionPackage.setId(1L);
        tuitionPackage.setName("12 sessions");
        tuitionPackage.setSessionsPerWeek(3);
        tuitionPackage.setTotalSessions(12);
        tuitionPackage.setExpectedMonths(1);
        tuitionPackage.setPrice(new BigDecimal("700000"));
        tuitionPackage.setStatus(TuitionPackageStatus.ACTIVE);

        when(tuitionPackageRepository.findById(1L)).thenReturn(Optional.of(tuitionPackage));
        when(tuitionPackageRepository.save(tuitionPackage)).thenReturn(tuitionPackage);

        TuitionPackageResponse response = tuitionPackageService.deactivate(1L);

        assertThat(response.status()).isEqualTo(TuitionPackageStatus.INACTIVE);
        verify(tuitionPackageRepository).findById(1L);
        verify(tuitionPackageRepository).save(tuitionPackage);
        verify(tuitionPackageRepository, never()).delete(any(TuitionPackage.class));
    }

    private TuitionPackageCreateRequest validCreateRequest(BigDecimal price) {
        return new TuitionPackageCreateRequest(
                "8 sessions",
                2,
                8,
                1,
                price,
                TuitionPackageStatus.ACTIVE
        );
    }
}
