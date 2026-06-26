package com.jat.app.service;

import com.jat.app.dto.job.CreateJobOpportunityRequest;
import com.jat.app.dto.job.JobOpportunityResponse;
import com.jat.app.dto.job.UpdateJobOpportunityRequest;
import com.jat.app.entity.JobOpportunity;
import com.jat.app.entity.JobOpportunityStatus;
import com.jat.app.repository.JobOpportunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobOpportunityService {

    private final JobOpportunityRepository jobOpportunityRepository;

    @Transactional(readOnly = true)
    public List<JobOpportunityResponse> findAll(JobOpportunityStatus status) {
        // Keep filtering in the repository so future larger inboxes use database indexes.
        List<JobOpportunity> opportunities = status == null
                ? jobOpportunityRepository.findAllByOrderByCreatedAtDesc()
                : jobOpportunityRepository.findAllByStatusOrderByCreatedAtDesc(status);

        return opportunities.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobOpportunityResponse findById(UUID id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public JobOpportunityResponse create(CreateJobOpportunityRequest request) {
        return toResponse(jobOpportunityRepository.save(new JobOpportunity(
                request.title(),
                request.company(),
                request.location(),
                request.sourceUrl(),
                request.sourceName(),
                request.notes()
        )));
    }

    @Transactional
    public JobOpportunityResponse update(UUID id, UpdateJobOpportunityRequest request) {
        JobOpportunity opportunity = findEntity(id);
        opportunity.updateDetails(
                request.title(),
                request.company(),
                request.location(),
                request.sourceUrl(),
                request.sourceName(),
                request.status(),
                request.notes()
        );

        return toResponse(opportunity);
    }

    @Transactional
    public void delete(UUID id) {
        if (!jobOpportunityRepository.existsById(id)) {
            throw new IllegalArgumentException("Job opportunity not found: " + id);
        }

        jobOpportunityRepository.deleteById(id);
    }

    private JobOpportunity findEntity(UUID id) {
        return jobOpportunityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job opportunity not found: " + id));
    }

    private JobOpportunityResponse toResponse(JobOpportunity opportunity) {
        // Keep mapping explicit so API changes are visible during review.
        return new JobOpportunityResponse(
                opportunity.getId(),
                opportunity.getTitle(),
                opportunity.getCompany(),
                opportunity.getLocation(),
                opportunity.getSourceUrl(),
                opportunity.getSourceName(),
                opportunity.getStatus(),
                opportunity.getNotes(),
                opportunity.getCreatedAt(),
                opportunity.getUpdatedAt()
        );
    }
}
