package com.englishcenter.makeupcredit;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MakeupCreditRepository extends JpaRepository<MakeupCredit, Long> {
    Optional<MakeupCredit> findByStudentIdAndSourceSessionIdAndReason(
            Long studentId,
            Long sourceSessionId,
            MakeupCreditReason reason
    );

    List<MakeupCredit> findBySourceSessionId(Long sourceSessionId);

    Page<MakeupCredit> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(c.creditSessions - c.usedSessions), 0)
            FROM MakeupCredit c
            WHERE c.student.id = :studentId
              AND c.classroom.id = :classroomId
              AND c.status = :availableStatus
            """)
    int sumAvailableMakeupSessions(
            @Param("studentId") Long studentId,
            @Param("classroomId") Long classroomId,
            @Param("availableStatus") MakeupCreditStatus availableStatus
    );
}
