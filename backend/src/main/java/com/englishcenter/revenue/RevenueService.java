package com.englishcenter.revenue;

import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.revenue.dto.RevenueSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevenueService {
    private final PaymentRepository paymentRepository;

    public RevenueService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public RevenueSummaryResponse getSummary() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        BigDecimal todayRevenue = paymentRepository.sumValidAmountBetween(today, today);
        BigDecimal monthRevenue = paymentRepository.sumValidAmountBetween(firstDayOfMonth, today);
        BigDecimal totalRevenue = paymentRepository.sumAllValidAmount();

        return new RevenueSummaryResponse(today, todayRevenue, monthRevenue, totalRevenue);
    }
}
