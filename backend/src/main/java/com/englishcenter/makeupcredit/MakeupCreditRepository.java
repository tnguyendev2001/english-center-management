package com.englishcenter.makeupcredit;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MakeupCreditRepository extends JpaRepository<MakeupCredit, Long> {
    Optional<MakeupCredit> findByStudentIdAndSourceSessionIdAndReason(
            Long studentId,
            Long sourceSessionId,
            MakeupCreditReason reason
    );

    Page<MakeupCredit> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
