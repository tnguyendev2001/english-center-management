package com.englishcenter.tuitionpackage;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TuitionPackageRepository extends JpaRepository<TuitionPackage, Long> {
    @Query("""
            SELECT tuitionPackage
            FROM TuitionPackage tuitionPackage
            WHERE LOWER(tuitionPackage.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<TuitionPackage> search(@Param("keyword") String keyword, Pageable pageable);
}
