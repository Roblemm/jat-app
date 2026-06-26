package com.jat.app.repository;

import com.jat.app.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    // Area-scoped goal lists support the main Goals page and keep context switching explicit.
    List<Goal> findAllByAreaIdOrderByCreatedAtDesc(UUID areaId);

    // Project filtering narrows an area without making project membership required for every goal.
    List<Goal> findAllByAreaIdAndProjectIdOrderByCreatedAtDesc(UUID areaId, UUID projectId);

    // The Today dashboard only surfaces active goals; paused/completed/archived goals belong in review views.
    @Query("""
            select g from Goal g
            where g.status = com.jat.app.entity.GoalStatus.ACTIVE
              and (:areaId is null or g.area.id = :areaId)
            order by g.createdAt desc
            """)
    List<Goal> findDashboardActiveGoals(@Param("areaId") UUID areaId);
}
