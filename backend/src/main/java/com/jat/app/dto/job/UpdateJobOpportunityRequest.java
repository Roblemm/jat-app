package com.jat.app.dto.job;

import com.jat.app.entity.JobOpportunityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Full updates keep the first API simple; application event history can be modeled separately later.
public record UpdateJobOpportunityRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 160) String company,
        @Size(max = 160) String location,
        @Size(max = 600) String sourceUrl,
        @Size(max = 120) String sourceName,
        @NotNull JobOpportunityStatus status,
        String notes
) {
}
