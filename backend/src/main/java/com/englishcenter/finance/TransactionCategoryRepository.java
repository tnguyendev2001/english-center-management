package com.englishcenter.finance;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, Long> {
    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    Optional<TransactionCategory> findByCodeIgnoreCase(String code);

    List<TransactionCategory> findAllByOrderByDirectionAscDisplayOrderAscNameAsc();

    List<TransactionCategory> findAllByDirectionAndActiveTrueOrderByDisplayOrderAscNameAsc(CategoryDirection direction);

    @Query("""
            SELECT COUNT(tx) > 0
            FROM CashTransaction tx
            WHERE tx.category.id = :categoryId
            """)
    boolean hasTransactions(@Param("categoryId") Long categoryId);
}
