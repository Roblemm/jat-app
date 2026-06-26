package com.jat.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_opportunities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobOpportunity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 160)
    private String company;

    @Column(length = 160)
    private String location;

    // Keep the original posting URL so the user can revisit the source of truth before applying.
    @Column(length = 600)
    private String sourceUrl;

    // Source name captures where the opportunity came from without needing an integration table yet.
    @Column(length = 120)
    private String sourceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobOpportunityStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public JobOpportunity(
            String title,
            String company,
            String location,
            String sourceUrl,
            String sourceName,
            String notes
    ) {
        this.id = UUID.randomUUID();
        this.status = JobOpportunityStatus.SAVED;
        updateDetails(title, company, location, sourceUrl, sourceName, this.status, notes);
    }

    // Keep all editable fields together so full-record updates cannot forget a related value.
    public void updateDetails(
            String title,
            String company,
            String location,
            String sourceUrl,
            String sourceName,
            JobOpportunityStatus status,
            String notes
    ) {
        this.title = title.trim();
        this.company = company.trim();
        this.location = blankToNull(location);
        this.sourceUrl = blankToNull(sourceUrl);
        this.sourceName = blankToNull(sourceName);
        this.status = status;
        this.notes = blankToNull(notes);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
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
