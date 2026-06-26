package com.jat.app.service;

import com.jat.app.dto.jobimport.ExtractionConfidence;
import com.jat.app.dto.jobimport.JobImportExtractedFields;
import com.jat.app.dto.jobimport.JobImportFieldConfidence;
import com.jat.app.dto.jobimport.JobImportPreviewRequest;
import com.jat.app.dto.jobimport.JobImportPreviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class JobImportService {

    private static final int DESCRIPTION_LIMIT = 2400;
    private static final Pattern HTML_TITLE_PATTERN = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");
    private static final Pattern H1_PATTERN = Pattern.compile("(?is)<h1[^>]*>(.*?)</h1>");

    private final JobPageFetcher jobPageFetcher;

    public JobImportPreviewResponse preview(JobImportPreviewRequest request) {
        String sourceUrl = blankToNull(request.sourceUrl());
        String pastedText = blankToNull(request.pastedText());

        if (sourceUrl == null && pastedText == null) {
            throw new IllegalArgumentException("sourceUrl or pastedText is required");
        }

        // Prefer pasted text because it works for logged-in pages and avoids bot-blocked fetches.
        String rawContent = pastedText != null ? pastedText : jobPageFetcher.fetch(sourceUrl);
        String readableText = normalizeWhitespace(stripHtml(rawContent));
        String sourceName = sourceUrl == null ? null : hostName(sourceUrl);

        ExtractedValue title = firstNonBlank(
                labeledValue(readableText, "title", ExtractionConfidence.HIGH),
                htmlValue(rawContent, H1_PATTERN, ExtractionConfidence.HIGH),
                splitTitleValue(rawContent, ExtractionConfidence.MEDIUM)
        );
        ExtractedValue company = firstNonBlank(
                labeledValue(readableText, "company", ExtractionConfidence.HIGH),
                companyFromTitle(rawContent, ExtractionConfidence.MEDIUM)
        );
        ExtractedValue location = labeledValue(readableText, "location", ExtractionConfidence.HIGH);

        JobImportExtractedFields extracted = new JobImportExtractedFields(
                title.value(),
                company.value(),
                location.value(),
                limit(readableText, DESCRIPTION_LIMIT),
                sourceName
        );
        JobImportFieldConfidence confidence = new JobImportFieldConfidence(
                title.confidence(),
                company.confidence(),
                location.confidence()
        );

        boolean needsReview = title.confidence() != ExtractionConfidence.HIGH
                || company.confidence() != ExtractionConfidence.HIGH
                || location.confidence() != ExtractionConfidence.HIGH;

        return new JobImportPreviewResponse(sourceUrl, extracted, confidence, needsReview);
    }

    private ExtractedValue labeledValue(String text, String label, ExtractionConfidence confidence) {
        Pattern pattern = Pattern.compile("(?im)^\\s*" + Pattern.quote(label) + "\\s*:\\s*(.+?)\\s*$");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return ExtractedValue.missing();
        }

        return new ExtractedValue(cleanExtractedValue(matcher.group(1)), confidence);
    }

    private ExtractedValue htmlValue(String rawContent, Pattern pattern, ExtractionConfidence confidence) {
        Matcher matcher = pattern.matcher(rawContent);
        if (!matcher.find()) {
            return ExtractedValue.missing();
        }

        return new ExtractedValue(cleanExtractedValue(stripHtml(matcher.group(1))), confidence);
    }

    private ExtractedValue splitTitleValue(String rawContent, ExtractionConfidence confidence) {
        ExtractedValue pageTitle = htmlValue(rawContent, HTML_TITLE_PATTERN, confidence);
        if (pageTitle.isMissing()) {
            return ExtractedValue.missing();
        }

        String[] pieces = pageTitle.value().split("\\s+[-|]\\s+", 2);
        return new ExtractedValue(cleanExtractedValue(pieces[0]), confidence);
    }

    private ExtractedValue companyFromTitle(String rawContent, ExtractionConfidence confidence) {
        ExtractedValue pageTitle = htmlValue(rawContent, HTML_TITLE_PATTERN, confidence);
        if (pageTitle.isMissing()) {
            return ExtractedValue.missing();
        }

        String[] pieces = pageTitle.value().split("\\s+[-|]\\s+", 2);
        if (pieces.length < 2) {
            return ExtractedValue.missing();
        }

        return new ExtractedValue(cleanExtractedValue(pieces[1]), confidence);
    }

    private ExtractedValue firstNonBlank(ExtractedValue... values) {
        for (ExtractedValue value : values) {
            if (!value.isMissing()) {
                return value;
            }
        }

        return ExtractedValue.missing();
    }

    private String stripHtml(String value) {
        return value
                .replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)</div>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    private String normalizeWhitespace(String value) {
        return value
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n\\s+", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String cleanExtractedValue(String value) {
        String cleaned = normalizeWhitespace(value).replaceAll("\\s+[-|]\\s+.*$", "").trim();
        return blankToNull(cleaned);
    }

    private String hostName(String sourceUrl) {
        try {
            String host = URI.create(sourceUrl).getHost();
            if (host == null) {
                return null;
            }

            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String limit(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength).trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private record ExtractedValue(String value, ExtractionConfidence confidence) {

        static ExtractedValue missing() {
            return new ExtractedValue(null, ExtractionConfidence.LOW);
        }

        boolean isMissing() {
            return value == null || value.isBlank();
        }
    }
}
