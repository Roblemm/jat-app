package com.jat.app.dto.jobimport;

// Preview response is intentionally not persisted; it feeds a review screen before saving to the inbox.
public record JobImportPreviewResponse(
        String sourceUrl,
        JobImportExtractedFields extracted,
        JobImportFieldConfidence confidence,
        boolean needsReview
) {
}
