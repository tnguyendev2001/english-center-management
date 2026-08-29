package com.englishcenter.studentpackage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "student_package_period_adjustments")
public class StudentPackagePeriodAdjustment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_package_id", nullable = false)
    private StudentPackage studentPackage;

    @Column(name = "old_effective_start_date")
    private LocalDate oldEffectiveStartDate;

    @Column(name = "new_manual_start_date", nullable = false)
    private LocalDate newManualStartDate;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;
}
