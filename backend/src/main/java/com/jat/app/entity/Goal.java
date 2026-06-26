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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "goals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Goal {

    @Id
    private UUID id;

    // Every goal belongs to an area so progress can be viewed through the same contexts as tasks.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    // Project is optional because some outcomes belong to an area without needing an extra sub-folder.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Stored as a string so database rows remain readable and stable across enum ordering changes.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GoalType goalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GoalStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Recurrence recurrence;

    // Used by TARGET goals, but left nullable so CHECKLIST and HABIT goals are not forced into fake numbers.
    @Column(precision = 12, scale = 2)
    private BigDecimal targetValue;

    // Human-readable unit for target goals, such as applications, hours, or sessions.
    @Column(length = 40)
    private String unit;

    private LocalDate targetDate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Goal(
            Area area,
            Project project,
            String title,
            String description,
            GoalType goalType,
            Recurrence recurrence,
            BigDecimal targetValue,
            String unit,
            LocalDate targetDate
    ) {
        this.id = UUID.randomUUID();
        this.area = area;
        this.project = project;
        this.title = title.trim();
        this.description = description;
        this.goalType = goalType;
        this.status = GoalStatus.ACTIVE;
        this.recurrence = recurrence;
        this.targetValue = targetValue;
        this.unit = unit;
        this.targetDate = targetDate;
    }

    public void changeStatus(GoalStatus status) {
        this.status = status;
    }

    // Hibernate calls this before the first insert so services do not repeat audit-field setup.
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
