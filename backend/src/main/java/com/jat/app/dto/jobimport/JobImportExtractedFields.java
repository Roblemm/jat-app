package com.jat.app.dto.jobimport;

// Extracted fields are proposed values only; the user confirms them before creating a saved opportunity.
public record JobImportExtractedFields(
        String title,
        String company,
        String location,
        String description,
        String sourceName
) {
}
