package com.jat.app.controller;

import com.jat.app.dto.job.CreateJobOpportunityRequest;
import com.jat.app.dto.job.JobOpportunityResponse;
import com.jat.app.dto.job.UpdateJobOpportunityRequest;
import com.jat.app.entity.JobOpportunityStatus;
import com.jat.app.service.JobOpportunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/job-opportunities")
@RequiredArgsConstructor
public class JobOpportunityController {

    // The controller owns the HTTP contract; the service owns persistence and pipeline rules.
    private final JobOpportunityService jobOpportunityService;

    // Status is optional so the same endpoint can power both the full inbox and filtered tabs.
    @GetMapping
    public List<JobOpportunityResponse> findAll(@RequestParam(required = false) JobOpportunityStatus status) {
        return jobOpportunityService.findAll(status);
    }

    // Detail reads let the frontend open one opportunity without reloading the whole inbox.
    @GetMapping("/{id}")
    public JobOpportunityResponse findById(@PathVariable UUID id) {
        return jobOpportunityService.findById(id);
    }

    // New opportunities start as SAVED in the service so quick capture stays low friction.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobOpportunityResponse create(@Valid @RequestBody CreateJobOpportunityRequest request) {
        return jobOpportunityService.create(request);
    }

    // Full updates are simple for the MVP and keep the API predictable for edit forms.
    @PutMapping("/{id}")
    public JobOpportunityResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateJobOpportunityRequest request
    ) {
        return jobOpportunityService.update(id, request);
    }

    // Deleting removes an inbox item entirely; archiving remains available through status updates.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        jobOpportunityService.delete(id);
    }
}
