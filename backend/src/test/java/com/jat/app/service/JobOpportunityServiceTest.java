package com.jat.app.service;

import com.jat.app.dto.job.CreateJobOpportunityRequest;
import com.jat.app.dto.job.UpdateJobOpportunityRequest;
import com.jat.app.entity.JobOpportunity;
import com.jat.app.entity.JobOpportunityStatus;
import com.jat.app.repository.JobOpportunityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobOpportunityServiceTest {

    @Mock
    private JobOpportunityRepository jobOpportunityRepository;

    @InjectMocks
    private JobOpportunityService jobOpportunityService;

    @Test
    void createDefaultsNewOpportunityToSavedStatus() {
        // New inbox items should be quick to capture; status can be updated after review.
        CreateJobOpportunityRequest request = new CreateJobOpportunityRequest(
                "Backend Developer Intern",
                "Example Systems",
                "Remote",
                "https://example.com/jobs/backend-intern",
                "Company careers page",
                "Looks aligned with Java and APIs."
        );

        when(jobOpportunityRepository.save(any(JobOpportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = jobOpportunityService.create(request);

        assertThat(result.title()).isEqualTo("Backend Developer Intern");
        assertThat(result.company()).isEqualTo("Example Systems");
        assertThat(result.status()).isEqualTo(JobOpportunityStatus.SAVED);
        assertThat(result.sourceUrl()).isEqualTo("https://example.com/jobs/backend-intern");
    }

    @Test
    void findAllReturnsAllOpportunitiesWhenNoStatusFilterIsProvided() {
        // The inbox defaults to all active tracking states so the frontend can decide grouping and tabs later.
        JobOpportunity opportunity = new JobOpportunity(
                "Backend Developer Intern",
                "Example Systems",
                "Remote",
                "https://example.com/jobs/backend-intern",
                "Company careers page",
                null
        );

        when(jobOpportunityRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(opportunity));

        var result = jobOpportunityService.findAll(null);

        assertThat(result).extracting("title").containsExactly("Backend Developer Intern");
    }

    @Test
    void findAllFiltersByStatusWhenProvided() {
        // Status filtering supports simple inbox views such as saved, applied, or interviewing.
        when(jobOpportunityRepository.findAllByStatusOrderByCreatedAtDesc(JobOpportunityStatus.APPLIED)).thenReturn(List.of());

        jobOpportunityService.findAll(JobOpportunityStatus.APPLIED);

        verify(jobOpportunityRepository).findAllByStatusOrderByCreatedAtDesc(JobOpportunityStatus.APPLIED);
    }

    @Test
    void findByIdReturnsOpportunity() {
        JobOpportunity opportunity = new JobOpportunity(
                "Backend Developer Intern",
                "Example Systems",
                "Remote",
                "https://example.com/jobs/backend-intern",
                "Company careers page",
                null
        );

        when(jobOpportunityRepository.findById(opportunity.getId())).thenReturn(Optional.of(opportunity));

        var result = jobOpportunityService.findById(opportunity.getId());

        assertThat(result.id()).isEqualTo(opportunity.getId());
    }

    @Test
    void updateChangesTrackedFieldsAndStatus() {
        // Updating the full inbox record keeps edits straightforward before we add application history.
        JobOpportunity opportunity = new JobOpportunity(
                "Backend Developer Intern",
                "Example Systems",
                "Remote",
                "https://example.com/jobs/backend-intern",
                "Company careers page",
                null
        );
        UpdateJobOpportunityRequest request = new UpdateJobOpportunityRequest(
                "Backend Engineer Intern",
                "Example Systems",
                "Hybrid",
                "https://example.com/jobs/backend-engineer-intern",
                "Referral",
                JobOpportunityStatus.APPLIED,
                "Applied with tailored resume."
        );

        when(jobOpportunityRepository.findById(opportunity.getId())).thenReturn(Optional.of(opportunity));

        var result = jobOpportunityService.update(opportunity.getId(), request);

        assertThat(result.title()).isEqualTo("Backend Engineer Intern");
        assertThat(result.location()).isEqualTo("Hybrid");
        assertThat(result.status()).isEqualTo(JobOpportunityStatus.APPLIED);
        assertThat(result.notes()).isEqualTo("Applied with tailored resume.");
    }

    @Test
    void deleteRemovesOpportunityById() {
        UUID id = UUID.randomUUID();
        when(jobOpportunityRepository.existsById(id)).thenReturn(true);

        jobOpportunityService.delete(id);

        verify(jobOpportunityRepository).deleteById(id);
    }

    @Test
    void findByIdRejectsMissingOpportunity() {
        UUID id = UUID.randomUUID();
        when(jobOpportunityRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobOpportunityService.findById(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Job opportunity not found");
    }
}
