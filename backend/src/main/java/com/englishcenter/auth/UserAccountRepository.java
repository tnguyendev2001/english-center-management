package com.englishcenter.auth;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByNormalizedUsername(String normalizedUsername);

    boolean existsByNormalizedUsername(String normalizedUsername);

    boolean existsByStudentId(Long studentId);

    boolean existsByTeacherId(Long teacherId);

    boolean existsByRole(AccountRole role);

    @Query("""
            SELECT account
            FROM UserAccount account
            WHERE (:role IS NULL OR account.role = :role)
              AND (:status IS NULL OR account.status = :status)
            """)
    Page<UserAccount> searchByRoleAndStatus(
            @Param("role") AccountRole role,
            @Param("status") AccountStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT account
            FROM UserAccount account
            WHERE (
                    LOWER(account.username) LIKE LOWER(CONCAT('%', :username, '%'))
                    OR LOWER(account.normalizedUsername) LIKE LOWER(CONCAT('%', :username, '%'))
                  )
              AND (:role IS NULL OR account.role = :role)
              AND (:status IS NULL OR account.status = :status)
            """)
    Page<UserAccount> search(
            @Param("username") String username,
            @Param("role") AccountRole role,
            @Param("status") AccountStatus status,
            Pageable pageable
    );
}
