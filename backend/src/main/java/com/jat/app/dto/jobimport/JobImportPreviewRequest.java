package com.jat.app.dto.jobimport;

import jakarta.validation.constraints.Size;

// The import preview accepts a URL, pasted page text, or both; the service validates that at least one is useful.
public record JobImportPreviewRequest(
        @Size(max = 1200) String sourceUrl,
        String pastedText
) {
}
