package com.jat.app.service;

import com.jat.app.dto.jobimport.ExtractionConfidence;
import com.jat.app.dto.jobimport.JobImportExtractedFields;
import com.jat.app.dto.jobimport.JobImportFieldConfidence;
import com.jat.app.dto.jobimport.JobImportPreviewRequest;
import com.jat.app.dto.jobimport.JobImportPreviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class JobImportService {

    private static final int DESCRIPTION_LIMIT = 2400;
    private static final Pattern HTML_TITLE_PATTERN = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");
    private static final Pattern H1_PATTERN = Pattern.compile("(?is)<h1[^>]*>(.*?)</h1>");
    private static final Pattern JOB_APPLICATION_TITLE_PATTERN = Pattern.compile("(?i)^Job Application for\\s+(.+?)\\s+at\\s+(.+)$");

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
                metaContent(rawContent, "og:title", ExtractionConfidence.HIGH),
                htmlValue(rawContent, H1_PATTERN, ExtractionConfidence.HIGH),
                titleFromJobApplicationTitle(rawContent, ExtractionConfidence.HIGH),
                splitTitleValue(rawContent, ExtractionConfidence.MEDIUM)
        );
        ExtractedValue company = firstNonBlank(
                labeledValue(readableText, "company", ExtractionConfidence.HIGH),
                companyFromJobApplicationTitle(rawContent, ExtractionConfidence.HIGH),
                companyFromTitle(rawContent, ExtractionConfidence.MEDIUM),
                companyFromGreenhouseUrl(sourceUrl, ExtractionConfidence.MEDIUM)
        );
        ExtractedValue location = firstNonBlank(
                labeledValue(readableText, "location", ExtractionConfidence.HIGH),
                greenhouseLocation(rawContent, sourceUrl, ExtractionConfidence.HIGH)
        );

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

    private ExtractedValue metaContent(String rawContent, String propertyName, ExtractionConfidence confidence) {
        // Open Graph and Twitter metadata often carry cleaner values than rendered page text.
        Pattern pattern = Pattern.compile(
                "(?is)<meta\\b(?=[^>]*(?:property|name)\\s*=\\s*['\"]" + Pattern.quote(propertyName) + "['\"])(?=[^>]*content\\s*=\\s*['\"]([^'\"]+)['\"])[^>]*>"
        );
        Matcher matcher = pattern.matcher(rawContent);
        if (!matcher.find()) {
            return ExtractedValue.missing();
        }

        return new ExtractedValue(cleanExtractedValue(matcher.group(1)), confidence);
    }

    private ExtractedValue splitTitleValue(String rawContent, ExtractionConfidence confidence) {
        ExtractedValue pageTitle = htmlValue(rawContent, HTML_TITLE_PATTERN, confidence);
        if (pageTitle.isMissing()) {
            return ExtractedValue.missing();
        }

        String[] pieces = pageTitle.value().split("\\s+[-|]\\s+", 2);
        return new ExtractedValue(cleanExtractedValue(pieces[0]), confidence);
    }

    private ExtractedValue titleFromJobApplicationTitle(String rawContent, ExtractionConfidence confidence) {
        ExtractedValue pageTitle = htmlValue(rawContent, HTML_TITLE_PATTERN, confidence);
        if (pageTitle.isMissing()) {
            return ExtractedValue.missing();
        }

        Matcher matcher = JOB_APPLICATION_TITLE_PATTERN.matcher(pageTitle.value());
        if (!matcher.find()) {
            return ExtractedValue.missing();
        }

        return new ExtractedValue(cleanExtractedValue(matcher.group(1)), confidence);
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

    private ExtractedValue companyFromJobApplicationTitle(String rawContent, ExtractionConfidence confidence) {
        ExtractedValue pageTitle = htmlValue(rawContent, HTML_TITLE_PATTERN, confidence);
        if (pageTitle.isMissing()) {
            return ExtractedValue.missing();
        }

        Matcher matcher = JOB_APPLICATION_TITLE_PATTERN.matcher(pageTitle.value());
        if (!matcher.find()) {
            return ExtractedValue.missing();
        }

        return new ExtractedValue(cleanExtractedValue(matcher.group(2)), confidence);
    }

    private ExtractedValue companyFromGreenhouseUrl(String sourceUrl, ExtractionConfidence confidence) {
        if (sourceUrl == null || !isGreenhouseUrl(sourceUrl)) {
            return ExtractedValue.missing();
        }

        try {
            URI uri = URI.create(sourceUrl);
            String path = uri.getPath();
            String[] pieces = path == null ? new String[0] : path.split("/");
            if (pieces.length < 2 || pieces[1].isBlank()) {
                return ExtractedValue.missing();
            }

            return new ExtractedValue(toDisplayName(pieces[1]), confidence);
        } catch (IllegalArgumentException exception) {
            return ExtractedValue.missing();
        }
    }

    private ExtractedValue greenhouseLocation(String rawContent, String sourceUrl, ExtractionConfidence confidence) {
        if (!isGreenhouseUrl(sourceUrl)) {
            return ExtractedValue.missing();
        }

        ExtractedValue ogDescription = metaContent(rawContent, "og:description", confidence);
        if (!ogDescription.isMissing()) {
            return ogDescription;
        }

        // Greenhouse renders the location in a dedicated block when metadata is unavailable.
        Pattern locationPattern = Pattern.compile("(?is)<[^>]+class\\s*=\\s*['\"][^'\"]*job__location[^'\"]*['\"][^>]*>(.*?)</[^>]+>");
        Matcher matcher = locationPattern.matcher(rawContent);
        if (!matcher.find()) {
            return ExtractedValue.missing();
        }

        String withoutIcons = matcher.group(1).replaceAll("(?is)<svg.*?</svg>", " ");
        return new ExtractedValue(cleanExtractedValue(stripHtml(withoutIcons)), confidence);
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
        return HtmlUtils.htmlUnescape(value)
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
        String cleaned = HtmlUtils.htmlUnescape(normalizeWhitespace(value)).trim();
        return blankToNull(cleaned);
    }

    private boolean isGreenhouseUrl(String sourceUrl) {
        if (sourceUrl == null) {
            return false;
        }

        String host = hostName(sourceUrl);
        return host != null && (host.equals("job-boards.greenhouse.io") || host.equals("boards.greenhouse.io"));
    }

    private String toDisplayName(String value) {
        String normalized = value.replace('-', ' ').replace('_', ' ').trim();
        if (normalized.isBlank()) {
            return null;
        }

        StringBuilder displayName = new StringBuilder();
        for (String word : normalized.split("\\s+")) {
            if (!displayName.isEmpty()) {
                displayName.append(' ');
            }

            displayName.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                displayName.append(word.substring(1));
            }
        }

        return displayName.toString();
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
