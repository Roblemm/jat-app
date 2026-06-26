package com.jat.app.repository;

import com.jat.app.entity.JobOpportunity;
import com.jat.app.entity.JobOpportunityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobOpportunityRepository extends JpaRepository<JobOpportunity, UUID> {

    // Default inbox order shows the newest saved opportunities first.
    List<JobOpportunity> findAllByOrderByCreatedAtDesc();

    // Status filtering supports focused views such as applied, interviewing, or archived.
    List<JobOpportunity> findAllByStatusOrderByCreatedAtDesc(JobOpportunityStatus status);
}
