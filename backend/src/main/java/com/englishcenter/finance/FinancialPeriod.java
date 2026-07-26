package com.englishcenter.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "financial_periods",
        uniqueConstraints = @UniqueConstraint(name = "uk_financial_periods_year_month", columnNames = {"year", "month"})
)
public class FinancialPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "month", nullable = false)
    private int month;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FinancialPeriodStatus status;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by", length = 100)
    private String closedBy;

    @Column(name = "reopened_at")
    private LocalDateTime reopenedAt;

    @Column(name = "reopened_by", length = 100)
    private String reopenedBy;

    @Column(name = "reopen_reason", length = 1000)
    private String reopenReason;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "opening_balance_snapshot", precision = 19, scale = 2)
    private BigDecimal openingBalanceSnapshot;

    @Column(name = "total_income_snapshot", precision = 19, scale = 2)
    private BigDecimal totalIncomeSnapshot;

    @Column(name = "total_expense_snapshot", precision = 19, scale = 2)
    private BigDecimal totalExpenseSnapshot;

    @Column(name = "closing_balance_snapshot", precision = 19, scale = 2)
    private BigDecimal closingBalanceSnapshot;

    @Column(name = "outstanding_debt_snapshot", precision = 19, scale = 2)
    private BigDecimal outstandingDebtSnapshot;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
