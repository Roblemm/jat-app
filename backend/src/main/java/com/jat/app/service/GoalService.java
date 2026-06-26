package com.jat.app.service;

import com.jat.app.dto.goal.CreateGoalRequest;
import com.jat.app.dto.goal.GoalResponse;
import com.jat.app.entity.Area;
import com.jat.app.entity.Goal;
import com.jat.app.entity.Project;
import com.jat.app.repository.AreaRepository;
import com.jat.app.repository.GoalRepository;
import com.jat.app.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final AreaRepository areaRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<GoalResponse> findAll(UUID areaId, UUID projectId) {
        // Project is an optional filter; area remains the required top-level context for all goal queries.
        List<Goal> goals = projectId == null
                ? goalRepository.findAllByAreaIdOrderByCreatedAtDesc(areaId)
                : goalRepository.findAllByAreaIdAndProjectIdOrderByCreatedAtDesc(areaId, projectId);

        return goals.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public GoalResponse create(CreateGoalRequest request) {
        // Validate the required area first so every goal has a trustworthy context.
        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new IllegalArgumentException("Area not found: " + request.areaId()));

        Project project = resolveProject(request.projectId(), area);

        return toResponse(goalRepository.save(new Goal(
                area,
                project,
                request.title(),
                request.description(),
                request.goalType(),
                request.recurrence(),
                request.targetValue(),
                request.unit(),
                request.targetDate()
        )));
    }

    private Project resolveProject(UUID projectId, Area area) {
        if (projectId == null) {
            return null;
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        // Cross-area links would make filters lie, so reject them at the service boundary.
        if (!project.getArea().getId().equals(area.getId())) {
            throw new IllegalArgumentException("Project does not belong to area: " + projectId);
        }

        return project;
    }

    private GoalResponse toResponse(Goal goal) {
        Project project = goal.getProject();

        // Keep mapping close to service logic until goal list/detail views diverge enough to need a mapper.
        return new GoalResponse(
                goal.getId(),
                goal.getArea().getId(),
                goal.getArea().getName(),
                project == null ? null : project.getId(),
                project == null ? null : project.getName(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getGoalType(),
                goal.getStatus(),
                goal.getRecurrence(),
                goal.getTargetValue(),
                goal.getUnit(),
                goal.getTargetDate(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}
