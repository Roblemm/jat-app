package com.jat.app.service;

import com.jat.app.dto.jobimport.JobImportPreviewRequest;
import com.jat.app.dto.jobimport.ExtractionConfidence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobImportServiceTest {

    @Mock
    private JobPageFetcher jobPageFetcher;

    @InjectMocks
    private JobImportService jobImportService;

    @Test
    void previewExtractsFieldsFromPastedTextWithoutFetchingUrl() {
        // Pasted text is the most reliable fallback for logged-in or bot-blocked job boards.
        JobImportPreviewRequest request = new JobImportPreviewRequest(
                "https://example.com/jobs/backend-intern",
                """
                        Title: Backend Developer Intern
                        Company: Example Systems
                        Location: Remote

                        Build Java APIs and work with PostgreSQL.
                        """
        );

        var result = jobImportService.preview(request);

        assertThat(result.extracted().title()).isEqualTo("Backend Developer Intern");
        assertThat(result.extracted().company()).isEqualTo("Example Systems");
        assertThat(result.extracted().location()).isEqualTo("Remote");
        assertThat(result.extracted().sourceName()).isEqualTo("example.com");
        assertThat(result.confidence().title()).isEqualTo(ExtractionConfidence.HIGH);
        assertThat(result.confidence().company()).isEqualTo(ExtractionConfidence.HIGH);
        assertThat(result.confidence().location()).isEqualTo(ExtractionConfidence.HIGH);
        assertThat(result.needsReview()).isFalse();
        verifyNoInteractions(jobPageFetcher);
    }

    @Test
    void previewFetchesAndExtractsWhenOnlyUrlIsProvided() {
        // URL-only import is the fastest path when a public posting exposes enough readable page text.
        String sourceUrl = "https://example.com/jobs/backend-intern";
        when(jobPageFetcher.fetch(sourceUrl)).thenReturn("""
                <html>
                  <head><title>Backend Developer Intern - Example Systems</title></head>
                  <body>
                    <h1>Backend Developer Intern</h1>
                    <p>Company: Example Systems</p>
                    <p>Location: Remote</p>
                    <p>Build Java APIs and work with PostgreSQL.</p>
                  </body>
                </html>
                """);

        var result = jobImportService.preview(new JobImportPreviewRequest(sourceUrl, null));

        assertThat(result.extracted().title()).isEqualTo("Backend Developer Intern");
        assertThat(result.extracted().company()).isEqualTo("Example Systems");
        assertThat(result.extracted().location()).isEqualTo("Remote");
        assertThat(result.extracted().sourceName()).isEqualTo("example.com");
        assertThat(result.needsReview()).isFalse();
    }

    @Test
    void previewExtractsGreenhouseStyleTitleCompanyAndLocation() {
        // Greenhouse job boards expose key fields through page metadata and layout classes,
        // not always through simple "Company:" or "Location:" labels in the visible text.
        String sourceUrl = "https://job-boards.greenhouse.io/example/jobs/123";
        when(jobPageFetcher.fetch(sourceUrl)).thenReturn("""
                <html>
                  <head>
                    <title>Job Application for Engineering Intern - Summer &#x27;26 at Example Security</title>
                    <meta property="og:title" content="Engineering Intern - Summer &#x27;26"/>
                    <meta property="og:description" content="Remote - US"/>
                  </head>
                  <body>
                    <h1 class="section-header section-header--large font-primary">Engineering Intern - Summer &#x27;26</h1>
                    <div class="job__location">Remote - US</div>
                    <p>Work with backend services and production data.</p>
                  </body>
                </html>
                """);

        var result = jobImportService.preview(new JobImportPreviewRequest(sourceUrl, null));

        assertThat(result.extracted().title()).isEqualTo("Engineering Intern - Summer '26");
        assertThat(result.extracted().company()).isEqualTo("Example Security");
        assertThat(result.extracted().location()).isEqualTo("Remote - US");
        assertThat(result.confidence().title()).isEqualTo(ExtractionConfidence.HIGH);
        assertThat(result.confidence().company()).isEqualTo(ExtractionConfidence.HIGH);
        assertThat(result.confidence().location()).isEqualTo(ExtractionConfidence.HIGH);
        assertThat(result.needsReview()).isFalse();
    }

    @Test
    void previewRequiresUrlOrPastedText() {
        // An import with no source gives the extractor nothing trustworthy to inspect.
        JobImportPreviewRequest request = new JobImportPreviewRequest(null, " ");

        assertThatThrownBy(() -> jobImportService.preview(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceUrl or pastedText is required");
    }
}
