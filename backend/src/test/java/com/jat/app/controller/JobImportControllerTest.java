package com.jat.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jat.app.dto.jobimport.ExtractionConfidence;
import com.jat.app.dto.jobimport.JobImportExtractedFields;
import com.jat.app.dto.jobimport.JobImportFieldConfidence;
import com.jat.app.dto.jobimport.JobImportPreviewRequest;
import com.jat.app.dto.jobimport.JobImportPreviewResponse;
import com.jat.app.service.JobImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobImportController.class)
class JobImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JobImportService jobImportService;

    @Test
    void previewReturnsExtractedJobFields() throws Exception {
        // The preview endpoint powers autofill without immediately saving uncertain extracted data.
        JobImportPreviewResponse response = new JobImportPreviewResponse(
                "https://example.com/jobs/backend-intern",
                new JobImportExtractedFields(
                        "Backend Developer Intern",
                        "Example Systems",
                        "Remote",
                        "Build Java APIs and work with PostgreSQL.",
                        "example.com"
                ),
                new JobImportFieldConfidence(
                        ExtractionConfidence.HIGH,
                        ExtractionConfidence.HIGH,
                        ExtractionConfidence.HIGH
                ),
                false
        );
        when(jobImportService.preview(any(JobImportPreviewRequest.class))).thenReturn(response);

        JobImportPreviewRequest request = new JobImportPreviewRequest(
                "https://example.com/jobs/backend-intern",
                null
        );

        mockMvc.perform(post("/api/job-imports/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extracted.title").value("Backend Developer Intern"))
                .andExpect(jsonPath("$.extracted.company").value("Example Systems"))
                .andExpect(jsonPath("$.confidence.title").value("HIGH"))
                .andExpect(jsonPath("$.needsReview").value(false));
    }

    @Test
    void previewReturnsBadRequestWhenSourceIsMissing() throws Exception {
        // Empty import requests are client input problems, so they should not surface as server errors.
        when(jobImportService.preview(any(JobImportPreviewRequest.class)))
                .thenThrow(new IllegalArgumentException("sourceUrl or pastedText is required"));

        JobImportPreviewRequest request = new JobImportPreviewRequest(null, null);

        mockMvc.perform(post("/api/job-imports/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("sourceUrl or pastedText is required"));
    }
}
