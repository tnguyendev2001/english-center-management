package com.englishcenter.invoice;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Page<Invoice> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<Invoice> findByEnrollmentId(Long enrollmentId);
}
