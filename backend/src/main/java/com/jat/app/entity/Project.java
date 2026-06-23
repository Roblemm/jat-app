package com.jat.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

    @Id
    private UUID id;

    // Projects only make sense inside an area; this keeps project names scoped to the user's current context.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    // Preserve the user's preferred casing for display while normalizedName handles comparisons.
    @Column(nullable = false, length = 120)
    private String name;

    // Lowercase/trimmed value used for case-insensitive uniqueness within an area.
    @Column(nullable = false, length = 120)
    private String normalizedName;

    // Timestamps are managed by the entity lifecycle so service code does not repeat audit-field setup.
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Project(Area area, String name) {
        this.id = UUID.randomUUID();
        this.area = area;
        rename(name);
    }

    // Store the display name and normalized name together so uniqueness matches autocomplete behavior.
    public void rename(String name) {
        String trimmedName = name.trim();
        this.name = trimmedName;
        this.normalizedName = normalizeName(trimmedName);
    }

    public static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    // Hibernate calls this before the first insert.
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
