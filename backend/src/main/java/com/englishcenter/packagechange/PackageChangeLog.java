package com.englishcenter.packagechange;

import com.englishcenter.classroom.Classroom;
import com.englishcenter.student.Student;
import com.englishcenter.studentpackage.StudentPackage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "package_change_logs")
public class PackageChangeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "old_student_package_id", nullable = false)
    private StudentPackage oldStudentPackage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "new_student_package_id", nullable = false)
    private StudentPackage newStudentPackage;

    @Column(name = "old_package_name", nullable = false)
    private String oldPackageName;

    @Column(name = "new_package_name", nullable = false)
    private String newPackageName;

    @Column(name = "old_total_sessions", nullable = false)
    private Integer oldTotalSessions;

    @Column(name = "new_total_sessions", nullable = false)
    private Integer newTotalSessions;

    @Column(name = "old_final_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal oldFinalAmount;

    @Column(name = "new_package_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal newPackagePrice;

    @Column(name = "used_sessions", nullable = false)
    private Integer usedSessions;

    @Column(name = "old_unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal oldUnitPrice;

    @Column(name = "used_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal usedAmount;

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "adjustment_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal adjustmentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, length = 20)
    private PackageChangeAdjustmentType adjustmentType;

    @Column(name = "new_invoice_final_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal newInvoiceFinalAmount;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    void prePersist() {
        changedAt = LocalDateTime.now();
    }
}
