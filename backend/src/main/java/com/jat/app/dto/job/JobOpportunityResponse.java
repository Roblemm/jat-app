package com.jat.app.dto.job;

import com.jat.app.entity.JobOpportunityStatus;

import java.time.Instant;
import java.util.UUID;

// The response is the stable API shape for the Job Inbox list and detail screens.
public record JobOpportunityResponse(
        UUID id,
        String title,
        String company,
        String location,
        String sourceUrl,
        String sourceName,
        JobOpportunityStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
