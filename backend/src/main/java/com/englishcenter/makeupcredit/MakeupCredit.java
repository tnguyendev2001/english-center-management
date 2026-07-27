package com.englishcenter.makeupcredit;

import com.englishcenter.classroom.Classroom;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.student.Student;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Leave-tracking record for V1 (user-facing: "Nghỉ phép").
 *
 * <p>Historically named MakeupCredit / "buổi bù", but V1 does <strong>not</strong> represent an
 * actual makeup/replacement class. When Attendance is EXCUSED, one AVAILABLE record is kept to
 * show approved leave; Enrollment usedSessions / remainingSessions / totalSessions are unchanged.
 *
 * <p>A future real replacement class should be a separate ClassSession-based feature named
 * "Buổi học bổ sung", not this entity. Table {@code makeup_credits} is retained for compatibility.
 */
@Getter
@Setter
@Entity
@Table(name = "makeup_credits")
public class MakeupCredit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_session_id")
    private ClassSession sourceSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private MakeupCreditReason reason;

    @Column(name = "credit_sessions", nullable = false)
    private Integer creditSessions;

    @Column(name = "used_sessions", nullable = false)
    private Integer usedSessions;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MakeupCreditStatus status;

    @Column(name = "note", length = 1000)
    private String note;

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
