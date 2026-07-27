package com.englishcenter.academic.lesson;

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
        name = "lesson_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lesson_records_session",
                columnNames = "class_session_id"
        )
)
public class LessonRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_session_id", nullable = false, unique = true)
    private Long classSessionId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "objectives", columnDefinition = "TEXT")
    private String objectives;

    @Column(name = "planned_content", columnDefinition = "TEXT")
    private String plannedContent;

    @Column(name = "actual_content", columnDefinition = "TEXT")
    private String actualContent;

    @Column(name = "vocabulary", columnDefinition = "TEXT")
    private String vocabulary;

    @Column(name = "grammar_topics", columnDefinition = "TEXT")
    private String grammarTopics;

    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    @Column(name = "homework_instruction", columnDefinition = "TEXT")
    private String homeworkInstruction;

    @Column(name = "teacher_note", columnDefinition = "TEXT")
    private String teacherNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LessonStatus status;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

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
