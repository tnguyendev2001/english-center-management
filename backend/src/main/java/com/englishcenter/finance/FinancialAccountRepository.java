package com.englishcenter.finance;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialAccountRepository extends JpaRepository<FinancialAccount, Long> {
    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    Optional<FinancialAccount> findByCodeIgnoreCase(String code);

    List<FinancialAccount> findAllByOrderByDisplayOrderAscNameAsc();

    List<FinancialAccount> findAllByActiveTrueOrderByDisplayOrderAscNameAsc();

    Optional<FinancialAccount> findFirstByTypeAndActiveTrueOrderByDisplayOrderAscIdAsc(FinancialAccountType type);

    @Query("""
            SELECT COUNT(tx) > 0
            FROM CashTransaction tx
            WHERE tx.account.id = :accountId
            """)
    boolean hasTransactions(@Param("accountId") Long accountId);
}
