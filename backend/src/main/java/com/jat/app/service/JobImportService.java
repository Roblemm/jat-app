package com.jat.app.service;

import com.jat.app.dto.jobimport.JobImportPreviewRequest;
import com.jat.app.dto.jobimport.JobImportPreviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobImportService {

    private final JobPageFetcher jobPageFetcher;
    private final JobImportExtractor jobImportExtractor = new JobImportExtractor();

    public JobImportPreviewResponse preview(JobImportPreviewRequest request) {
        String sourceUrl = blankToNull(request.sourceUrl());
        String pastedText = blankToNull(request.pastedText());

        if (sourceUrl == null && pastedText == null) {
            throw new IllegalArgumentException("sourceUrl or pastedText is required");
        }

        // Pasted text is preferred because it works for logged-in pages and bot-blocked job boards.
        String rawContent = pastedText != null ? pastedText : jobPageFetcher.fetch(sourceUrl);
        return jobImportExtractor.preview(sourceUrl, rawContent);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
