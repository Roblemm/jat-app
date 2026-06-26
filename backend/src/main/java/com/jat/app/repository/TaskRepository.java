package com.jat.app.repository;

import com.jat.app.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    // Area-scoped lists power the default task inbox and prevent unrelated contexts from mixing.
    List<Task> findAllByAreaIdOrderByCreatedAtDesc(UUID areaId);

    // Project filtering supports focused task lists without making projects mandatory.
    List<Task> findAllByAreaIdAndProjectIdOrderByCreatedAtDesc(UUID areaId, UUID projectId);

    // Goal filtering supports progress views where tasks become the checklist for a goal.
    List<Task> findAllByAreaIdAndGoalIdOrderByCreatedAtDesc(UUID areaId, UUID goalId);

    // When both filters are present, keep the query explicit instead of relying on in-memory filtering.
    List<Task> findAllByAreaIdAndProjectIdAndGoalIdOrderByCreatedAtDesc(UUID areaId, UUID projectId, UUID goalId);
}
