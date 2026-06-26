package com.jat.app.dto.jobimport;

// Field-level confidence is more useful than one global score because title and company may be clearer than location.
public record JobImportFieldConfidence(
        ExtractionConfidence title,
        ExtractionConfidence company,
        ExtractionConfidence location
) {
}
