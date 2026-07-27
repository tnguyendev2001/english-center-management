package com.englishcenter.academic.evaluation;

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
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "student_evaluations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_student_evaluations",
                columnNames = {"evaluation_period_id", "classroom_id", "student_id"}
        )
)
public class StudentEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_period_id", nullable = false)
    private Long evaluationPeriodId;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "areas_for_improvement", columnDefinition = "TEXT")
    private String areasForImprovement;

    @Column(name = "learning_attitude", columnDefinition = "TEXT")
    private String learningAttitude;

    @Column(name = "participation", columnDefinition = "TEXT")
    private String participation;

    @Column(name = "homework_performance", columnDefinition = "TEXT")
    private String homeworkPerformance;

    @Column(name = "teacher_comment", columnDefinition = "TEXT")
    private String teacherComment;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "internal_note", columnDefinition = "TEXT")
    private String internalNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_rating", length = 30)
    private OverallRating overallRating;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StudentEvaluationStatus status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @Column(name = "reopen_reason", length = 1000)
    private String reopenReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

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
