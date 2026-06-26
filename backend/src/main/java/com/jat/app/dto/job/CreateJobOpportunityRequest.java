package com.jat.app.dto.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Creation favors quick capture: status starts as SAVED and can move through the pipeline later.
public record CreateJobOpportunityRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 160) String company,
        @Size(max = 160) String location,
        @Size(max = 600) String sourceUrl,
        @Size(max = 120) String sourceName,
        String notes
) {
}
