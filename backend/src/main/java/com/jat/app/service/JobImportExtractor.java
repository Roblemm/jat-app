package com.jat.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jat.app.dto.jobimport.ExtractionConfidence;
import com.jat.app.dto.jobimport.JobImportExtractedFields;
import com.jat.app.dto.jobimport.JobImportFieldConfidence;
import com.jat.app.dto.jobimport.JobImportPreviewResponse;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class JobImportExtractor {

    private static final int DESCRIPTION_LIMIT = 2400;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern HTML_TITLE_PATTERN = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");
    private static final Pattern H1_PATTERN = Pattern.compile("(?is)<h1[^>]*>(.*?)</h1>");
    private static final Pattern JOB_APPLICATION_TITLE_PATTERN = Pattern.compile("(?i)^Job Application for\\s+(.+?)\\s+at\\s+(.+)$");
    private static final Pattern LINKEDIN_TITLE_PATTERN = Pattern.compile("(?i)^(.+?)\\s+hiring\\s+(.+?)(?:\\s+in\\s+(.+?))?\\s*\\|\\s*LinkedIn$");

    JobImportPreviewResponse preview(String sourceUrl, String rawContent) {
        String readableText = normalizeWhitespace(stripHtml(rawContent));
        String sourceName = sourceUrl == null ? null : hostName(sourceUrl);
        StructuredJobPosting structuredPosting = structuredJobPosting(rawContent);

        // Field order matters: explicit labels and structured JobPosting data beat fuzzy title splitting.
        ExtractedValue title = firstNonBlank(
                labeledValue(readableText, "title", ExtractionConfidence.HIGH),
                structuredValue(structuredPosting.title(), ExtractionConfidence.HIGH),
                linkedInTitle(rawContent, sourceUrl, ExtractionConfidence.HIGH),
                metaContent(rawContent, "og:title", ExtractionConfidence.HIGH),
                htmlValue(rawContent, H1_PATTERN, ExtractionConfidence.HIGH),
                titleFromJobApplicationTitle(rawContent, ExtractionConfidence.HIGH),
                titleFromAtTitle(rawContent, ExtractionConfidence.HIGH),
                splitTitleValue(rawContent, ExtractionConfidence.MEDIUM)
        );
        ExtractedValue company = firstNonBlank(
                labeledValue(readableText, "company", ExtractionConfidence.HIGH),
                structuredValue(structuredPosting.company(), ExtractionConfidence.HIGH),
                microdataCompany(rawContent, ExtractionConfidence.HIGH),
                linkedInCompany(rawContent, sourceUrl, ExtractionConfidence.HIGH),
                companyFromJobApplicationTitle(rawContent, ExtractionConfidence.HIGH),
                companyFromAtTitle(rawContent, ExtractionConfidence.HIGH),
                companyFromTitle(rawContent, ExtractionConfidence.MEDIUM),
                companyFromGreenhouseUrl(sourceUrl, ExtractionConfidence.MEDIUM)
        );
        ExtractedValue location = firstNonBlank(
                labeledValue(readableText, "location", ExtractionConfidence.HIGH),
                structuredValue(structuredPosting.location(), ExtractionConfidence.HIGH),
                microdataLocation(rawContent, ExtractionConfidence.HIGH),
                linkedInLocation(rawContent, sourceUrl, ExtractionConfidence.HIGH),
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

    private ExtractedValue structuredValue(String value, ExtractionConfidence confidence) {
        return value == null ? ExtractedValue.missing() : new ExtractedValue(cleanExtractedValue(value), confidence);
    }

    private ExtractedValue labeledValue(String text, String label, ExtractionConfidence confidence) {
        Pattern pattern = Pattern.compile("(?im)^\\s*" + Pattern.quote(label) + "\\s*:\\s*(.+?)\\s*$");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return ExtractedValue.missing();
        }

        return new ExtractedValue(cleanExtractedValue(matcher.group(1)), confidence);
    }

    private StructuredJobPosting structuredJobPosting(String rawContent) {
        for (String scriptBody : jsonLdScriptBodies(rawContent)) {
            StructuredJobPosting posting = structuredJobPostingFromJson(scriptBody);
            if (!posting.isEmpty()) {
                return posting;
            }
        }

        return StructuredJobPosting.empty();
    }

    private List<String> jsonLdScriptBodies(String rawContent) {
        Pattern pattern = Pattern.compile("(?is)<script\\b(?=[^>]*type\\s*=\\s*['\"]application/ld\\+json['\"])[^>]*>(.*?)</script>");
        Matcher matcher = pattern.matcher(rawContent);
        List<String> bodies = new ArrayList<>();
        while (matcher.find()) {
            bodies.add(HtmlUtils.htmlUnescape(matcher.group(1)).trim());
        }

        return bodies;
    }

    private StructuredJobPosting structuredJobPostingFromJson(String json) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            JsonNode jobPosting = findJobPostingNode(root);
            if (jobPosting == null) {
                return StructuredJobPosting.empty();
            }

            String title = textAt(jobPosting, "title");
            String company = organizationName(jobPosting.get("hiringOrganization"));
            String location = jobLocation(jobPosting);
            return new StructuredJobPosting(title, company, location);
        } catch (Exception exception) {
            // Bad JSON-LD should not break autofill; lower-confidence fallbacks still get a chance.
            return StructuredJobPosting.empty();
        }
    }

    private JsonNode findJobPostingNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        if (isJobPostingType(node.get("@type"))) {
            return node;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode match = findJobPostingNode(child);
                if (match != null) {
                    return match;
                }
            }
        }

        JsonNode graph = node.get("@graph");
        if (graph != null) {
            return findJobPostingNode(graph);
        }

        return null;
    }

    private boolean isJobPostingType(JsonNode typeNode) {
        if (typeNode == null || typeNode.isNull()) {
            return false;
        }

        if (typeNode.isTextual()) {
            return "JobPosting".equalsIgnoreCase(typeNode.asText());
        }

        if (typeNode.isArray()) {
            for (JsonNode child : typeNode) {
                if (isJobPostingType(child)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String organizationName(JsonNode organizationNode) {
        if (organizationNode == null || organizationNode.isNull()) {
            return null;
        }

        if (organizationNode.isTextual()) {
            return organizationNode.asText();
        }

        if (organizationNode.isArray() && !organizationNode.isEmpty()) {
            return organizationName(organizationNode.get(0));
        }

        return textAt(organizationNode, "name");
    }

    private String jobLocation(JsonNode jobPosting) {
        if ("TELECOMMUTE".equalsIgnoreCase(textAt(jobPosting, "jobLocationType"))) {
            return "Remote";
        }

        JsonNode locationNode = jobPosting.get("jobLocation");
        if (locationNode == null || locationNode.isNull()) {
            return null;
        }

        if (locationNode.isArray() && !locationNode.isEmpty()) {
            return jobLocationFromPlace(locationNode.get(0));
        }

        return jobLocationFromPlace(locationNode);
    }

    private String jobLocationFromPlace(JsonNode placeNode) {
        String placeName = textAt(placeNode, "name");
        if (placeName != null) {
            return placeName;
        }

        JsonNode addressNode = placeNode.get("address");
        if (addressNode == null || addressNode.isNull()) {
            return null;
        }

        if (addressNode.isTextual()) {
            return addressNode.asText();
        }

        String locality = textAt(addressNode, "addressLocality");
        String region = textAt(addressNode, "addressRegion");
        String country = textAt(addressNode, "addressCountry");
        return joinNonBlank(", ", locality, region, country);
    }

    private String textAt(JsonNode node, String fieldName) {
        if (node == null || node.get(fieldName) == null || node.get(fieldName).isNull()) {
            return null;
        }

        JsonNode value = node.get(fieldName);
        return value.isTextual() ? blankToNull(value.asText()) : null;
    }

    private ExtractedValue htmlValue(String rawContent, Pattern pattern, ExtractionConfidence confidence) {
        Matcher matcher = pattern.matcher(rawContent);
        if (!matcher.find()) {
            return ExtractedValue.missing();
        }

        return new ExtractedValue(cleanExtractedValue(stripHtml(matcher.group(1))), confidence);
    }

    private ExtractedValue metaContent(String rawContent, String propertyName, ExtractionConfidence confidence) {
        // Metadata often carries cleaner values than visible text, especially on JavaScript-heavy pages.
        Pattern pattern = Pattern.compile(
                "(?is)<meta\\b(?=[^>]*(?:property|name)\\s*=\\s*['\"]" + Pattern.quote(propertyName) + "['\"])(?=[^>]*content\\s*=\\s*['\"]([^'\"]+)['\"])[^>]*>"
        );
        Matcher matcher = pattern.matcher(rawContent);
        if (!matcher.find()) {
            return ExtractedValue.missing();
        }

        return new ExtractedValue(cleanExtractedValue(matcher.group(1)), confidence);
    }

    private ExtractedValue microdataCompany(String rawContent, ExtractionConfidence confidence) {
        // SmartRecruiters and older career pages may use schema.org microdata instead of JSON-LD.
        Pattern organizationPattern = Pattern.compile("(?is)<[^>]+itemprop\\s*=\\s*['\"]hiringOrganization['\"][^>]*>.*?</[^>]+>");
        Matcher organizationMatcher = organizationPattern.matcher(rawContent);
        if (!organizationMatcher.find()) {
            return ExtractedValue.missing();
        }

        String company = metaItempropContent(organizationMatcher.group(), "name");
        return company == null ? ExtractedValue.missing() : new ExtractedValue(cleanExtractedValue(company), confidence);
    }

    private ExtractedValue microdataLocation(String rawContent, ExtractionConfidence confidence) {
        Pattern formattedAddressPattern = Pattern.compile("(?is)formattedAddress\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher formattedAddressMatcher = formattedAddressPattern.matcher(rawContent);
        if (formattedAddressMatcher.find()) {
            return new ExtractedValue(cleanExtractedValue(formattedAddressMatcher.group(1)), confidence);
        }

        String locality = metaItempropContent(rawContent, "addressLocality");
        String region = metaItempropContent(rawContent, "addressRegion");
        String country = metaItempropContent(rawContent, "addressCountry");
        String location = joinNonBlank(", ", locality, region, country);
        return location == null ? ExtractedValue.missing() : new ExtractedValue(cleanExtractedValue(location), confidence);
    }

    private String metaItempropContent(String rawContent, String itemprop) {
        Pattern pattern = Pattern.compile(
                "(?is)<meta\\b(?=[^>]*itemprop\\s*=\\s*['\"]" + Pattern.quote(itemprop) + "['\"])(?=[^>]*content\\s*=\\s*['\"]([^'\"]+)['\"])[^>]*>"
        );
        Matcher matcher = pattern.matcher(rawContent);
        return matcher.find() ? matcher.group(1) : null;
    }

    private ExtractedValue linkedInTitle(String rawContent, String sourceUrl, ExtractionConfidence confidence) {
        Matcher matcher = linkedInTitleMatcher(rawContent, sourceUrl);
        if (matcher == null) {
            return ExtractedValue.missing();
        }

        return new ExtractedValue(cleanExtractedValue(matcher.group(2)), confidence);
    }

    private ExtractedValue linkedInCompany(String rawContent, String sourceUrl, ExtractionConfidence confidence) {
        Matcher matcher = linkedInTitleMatcher(rawContent, sourceUrl);
        if (matcher == null) {
            return ExtractedValue.missing();
        }

        return new ExtractedValue(cleanExtractedValue(matcher.group(1)), confidence);
    }

    private ExtractedValue linkedInLocation(String rawContent, String sourceUrl, ExtractionConfidence confidence) {
        Matcher titleMatcher = linkedInTitleMatcher(rawContent, sourceUrl);
        if (titleMatcher != null && titleMatcher.group(3) != null) {
            return new ExtractedValue(cleanExtractedValue(titleMatcher.group(3)), confidence);
        }

        ExtractedValue description = firstNonBlank(
                metaContent(rawContent, "description", ExtractionConfidence.MEDIUM),
                metaContent(rawContent, "og:description", ExtractionConfidence.MEDIUM)
        );
        if (description.isMissing()) {
            return ExtractedValue.missing();
        }

        Pattern locationPattern = Pattern.compile("(?i)\\bLocation:\\s*(.+?)(?:\\s+Type:|\\s+Employment type:|$)");
        Matcher locationMatcher = locationPattern.matcher(description.value());
        if (!locationMatcher.find()) {
            return ExtractedValue.missing();
        }

        return new ExtractedValue(cleanExtractedValue(locationMatcher.group(1)), confidence);
    }

    private Matcher linkedInTitleMatcher(String rawContent, String sourceUrl) {
        if (!isHost(sourceUrl, "linkedin.com")) {
            return null;
        }

        ExtractedValue pageTitle = firstNonBlank(
                htmlValue(rawContent, HTML_TITLE_PATTERN, ExtractionConfidence.HIGH),
                metaContent(rawContent, "og:title", ExtractionConfidence.HIGH)
        );
        if (pageTitle.isMissing()) {
            return null;
        }

        Matcher matcher = LINKEDIN_TITLE_PATTERN.matcher(pageTitle.value());
        return matcher.find() ? matcher : null;
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
        Matcher matcher = jobApplicationTitleMatcher(rawContent, confidence);
        if (matcher == null) {
            return ExtractedValue.missing();
        }

        return new ExtractedValue(cleanExtractedValue(matcher.group(1)), confidence);
    }

    private ExtractedValue titleFromAtTitle(String rawContent, ExtractionConfidence confidence) {
        String[] pieces = titlePiecesAroundAt(rawContent);
        if (pieces.length < 2) {
            return ExtractedValue.missing();
        }

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

    private ExtractedValue companyFromJobApplicationTitle(String rawContent, ExtractionConfidence confidence) {
        Matcher matcher = jobApplicationTitleMatcher(rawContent, confidence);
        if (matcher == null) {
            return ExtractedValue.missing();
        }

        return new ExtractedValue(cleanExtractedValue(matcher.group(2)), confidence);
    }

    private Matcher jobApplicationTitleMatcher(String rawContent, ExtractionConfidence confidence) {
        ExtractedValue pageTitle = htmlValue(rawContent, HTML_TITLE_PATTERN, confidence);
        if (pageTitle.isMissing()) {
            return null;
        }

        Matcher matcher = JOB_APPLICATION_TITLE_PATTERN.matcher(pageTitle.value());
        return matcher.find() ? matcher : null;
    }

    private ExtractedValue companyFromAtTitle(String rawContent, ExtractionConfidence confidence) {
        String[] pieces = titlePiecesAroundAt(rawContent);
        if (pieces.length < 2) {
            return ExtractedValue.missing();
        }

        return new ExtractedValue(cleanExtractedValue(pieces[1]), confidence);
    }

    private String[] titlePiecesAroundAt(String rawContent) {
        ExtractedValue pageTitle = htmlValue(rawContent, HTML_TITLE_PATTERN, ExtractionConfidence.HIGH);
        if (pageTitle.isMissing()) {
            return new String[0];
        }

        return pageTitle.value().split("\\s+@\\s+", 2);
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
        return isHost(sourceUrl, "job-boards.greenhouse.io") || isHost(sourceUrl, "boards.greenhouse.io");
    }

    private boolean isHost(String sourceUrl, String expectedHost) {
        if (sourceUrl == null) {
            return false;
        }

        String host = hostName(sourceUrl);
        return host != null && (host.equals(expectedHost) || host.endsWith("." + expectedHost));
    }

    private String joinNonBlank(String delimiter, String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            String cleaned = blankToNull(value);
            if (cleaned != null) {
                parts.add(cleaned);
            }
        }

        return parts.isEmpty() ? null : String.join(delimiter, parts);
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

    private record StructuredJobPosting(String title, String company, String location) {

        static StructuredJobPosting empty() {
            return new StructuredJobPosting(null, null, null);
        }

        boolean isEmpty() {
            return title == null && company == null && location == null;
        }
    }
}
