package com.jat.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jat.app.dto.job.CreateJobOpportunityRequest;
import com.jat.app.dto.job.JobOpportunityResponse;
import com.jat.app.dto.job.UpdateJobOpportunityRequest;
import com.jat.app.entity.JobOpportunityStatus;
import com.jat.app.service.JobOpportunityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobOpportunityController.class)
class JobOpportunityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JobOpportunityService jobOpportunityService;

    @Test
    void findAllReturnsJobOpportunities() throws Exception {
        // The inbox list supports optional status filtering, but defaults to all tracked opportunities.
        JobOpportunityResponse response = response(UUID.randomUUID(), JobOpportunityStatus.SAVED);
        when(jobOpportunityService.findAll(null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/job-opportunities"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].title").value("Backend Developer Intern"))
                .andExpect(jsonPath("$[0].company").value("Example Systems"))
                .andExpect(jsonPath("$[0].status").value("SAVED"));
    }

    @Test
    void findAllAcceptsStatusFilter() throws Exception {
        // Status query params let the frontend build saved/applied/interviewing tabs without new endpoints.
        when(jobOpportunityService.findAll(JobOpportunityStatus.APPLIED)).thenReturn(List.of());

        mockMvc.perform(get("/api/job-opportunities").queryParam("status", "APPLIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void findByIdReturnsJobOpportunity() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobOpportunityService.findById(id)).thenReturn(response(id, JobOpportunityStatus.SAVED));

        mockMvc.perform(get("/api/job-opportunities/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Backend Developer Intern"));
    }

    @Test
    void createReturnsCreatedOpportunity() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobOpportunityService.create(any(CreateJobOpportunityRequest.class)))
                .thenReturn(response(id, JobOpportunityStatus.SAVED));

        CreateJobOpportunityRequest request = new CreateJobOpportunityRequest(
                "Backend Developer Intern",
                "Example Systems",
                "Remote",
                "https://example.com/jobs/backend-intern",
                "Company careers page",
                "Looks aligned with Java and APIs."
        );

        mockMvc.perform(post("/api/job-opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("SAVED"));
    }

    @Test
    void updateReturnsUpdatedOpportunity() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobOpportunityService.update(any(UUID.class), any(UpdateJobOpportunityRequest.class)))
                .thenReturn(response(id, JobOpportunityStatus.APPLIED));

        UpdateJobOpportunityRequest request = new UpdateJobOpportunityRequest(
                "Backend Developer Intern",
                "Example Systems",
                "Remote",
                "https://example.com/jobs/backend-intern",
                "Company careers page",
                JobOpportunityStatus.APPLIED,
                "Applied with tailored resume."
        );

        mockMvc.perform(put("/api/job-opportunities/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(jobOpportunityService).delete(id);

        mockMvc.perform(delete("/api/job-opportunities/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void createRequiresTitle() throws Exception {
        // Title is required because an inbox item without a role name is not useful to review later.
        CreateJobOpportunityRequest request = new CreateJobOpportunityRequest(
                null,
                "Example Systems",
                "Remote",
                "https://example.com/jobs/backend-intern",
                "Company careers page",
                null
        );

        mockMvc.perform(post("/api/job-opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private JobOpportunityResponse response(UUID id, JobOpportunityStatus status) {
        return new JobOpportunityResponse(
                id,
                "Backend Developer Intern",
                "Example Systems",
                "Remote",
                "https://example.com/jobs/backend-intern",
                "Company careers page",
                status,
                "Looks aligned with Java and APIs.",
                Instant.now(),
                Instant.now()
        );
    }
}
