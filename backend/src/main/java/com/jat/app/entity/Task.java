package com.jat.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    @Id
    private UUID id;

    // Every task belongs to an area so the app can separate work contexts by default.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    // Project is optional; quick capture should not require choosing a sub-context.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    // Goal is optional because some tasks are standalone chores or ideas, not progress against an outcome.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id")
    private Goal goal;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Stored as strings so database rows stay readable and enum ordering changes cannot corrupt data.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Recurrence recurrence;

    // dueAt is a deadline, while remindAt is when the app should bring the task back to attention.
    private Instant dueAt;

    private Instant remindAt;

    // Scheduling fields are separate from dueAt so calendar blocks do not imply a deadline.
    private Instant scheduledStart;

    private Instant scheduledEnd;

    private Instant completedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Task(
            Area area,
            Project project,
            Goal goal,
            String title,
            String description,
            TaskType taskType,
            TaskPriority priority,
            Recurrence recurrence,
            Instant dueAt,
            Instant remindAt,
            Instant scheduledStart,
            Instant scheduledEnd
    ) {
        this.id = UUID.randomUUID();
        this.area = area;
        this.project = project;
        this.goal = goal;
        this.title = title.trim();
        this.description = description;
        this.taskType = taskType;
        this.status = TaskStatus.TODO;
        this.priority = priority;
        this.recurrence = recurrence;
        this.dueAt = dueAt;
        this.remindAt = remindAt;
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
    }

    // Hibernate calls this before the first insert so services do not duplicate timestamp setup.
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Hibernate calls this before updates so updatedAt reflects the last persisted edit.
    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
