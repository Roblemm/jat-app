package com.jat.app.repository;

import com.jat.app.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    // Area-scoped goal lists support the main Goals page and keep context switching explicit.
    List<Goal> findAllByAreaIdOrderByCreatedAtDesc(UUID areaId);

    // Project filtering narrows an area without making project membership required for every goal.
    List<Goal> findAllByAreaIdAndProjectIdOrderByCreatedAtDesc(UUID areaId, UUID projectId);
}
