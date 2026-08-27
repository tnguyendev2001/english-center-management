package com.englishcenter.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class VndAmountInWordsTest {
    @Test
    void convertsCommonTuitionAmounts() {
        assertThat(VndAmountInWords.convert(new BigDecimal("500000")))
                .isEqualTo("Năm trăm nghìn đồng");
        assertThat(VndAmountInWords.convert(new BigDecimal("1500000")))
                .isEqualTo("Một triệu năm trăm nghìn đồng");
        assertThat(VndAmountInWords.convert(new BigDecimal("1230000")))
                .isEqualTo("Một triệu hai trăm ba mươi nghìn đồng");
        assertThat(VndAmountInWords.convert(new BigDecimal("2000000")))
                .isEqualTo("Hai triệu đồng");
    }

    @Test
    void handlesZeroAndExactDecimalStorage() {
        assertThat(VndAmountInWords.convert(new BigDecimal("0.00"))).isEqualTo("Không đồng");
        assertThat(VndAmountInWords.convert(new BigDecimal("1005.00")))
                .isEqualTo("Một nghìn không trăm lẻ năm đồng");
    }

    @Test
    void rejectsFractionalOrNegativeVnd() {
        assertThatThrownBy(() -> VndAmountInWords.convert(new BigDecimal("10.50")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VndAmountInWords.convert(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
