package com.englishcenter.finance;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialPeriodRepository extends JpaRepository<FinancialPeriod, Long> {
    Optional<FinancialPeriod> findByYearAndMonth(int year, int month);

    List<FinancialPeriod> findAllByOrderByYearDescMonthDesc();

    List<FinancialPeriod> findAllByYearOrderByMonthAsc(int year);

    boolean existsByYearAndMonthAndStatus(int year, int month, FinancialPeriodStatus status);

    Optional<FinancialPeriod> findFirstByOrderByYearDescMonthDesc();
}
