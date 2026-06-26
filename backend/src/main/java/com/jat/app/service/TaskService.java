package com.jat.app.service;

import com.jat.app.dto.task.CreateTaskRequest;
import com.jat.app.dto.task.TaskResponse;
import com.jat.app.entity.Area;
import com.jat.app.entity.Goal;
import com.jat.app.entity.Project;
import com.jat.app.entity.Task;
import com.jat.app.entity.TaskStatus;
import com.jat.app.repository.AreaRepository;
import com.jat.app.repository.GoalRepository;
import com.jat.app.repository.ProjectRepository;
import com.jat.app.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final AreaRepository areaRepository;
    private final ProjectRepository projectRepository;
    private final GoalRepository goalRepository;

    @Transactional(readOnly = true)
    public List<TaskResponse> findAll(UUID areaId, UUID projectId, UUID goalId) {
        // Keep filtering in the repository so database indexes can do the work as task volume grows.
        List<Task> tasks = findTasks(areaId, projectId, goalId);

        return tasks.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        // Area is resolved first because every optional link must be validated against it.
        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new IllegalArgumentException("Area not found: " + request.areaId()));

        Project project = resolveProject(request.projectId(), area);
        Goal goal = resolveGoal(request.goalId(), area);

        return toResponse(taskRepository.save(new Task(
                area,
                project,
                goal,
                request.title(),
                request.description(),
                request.taskType(),
                request.priority(),
                request.recurrence(),
                request.dueAt(),
                request.remindAt(),
                request.scheduledStart(),
                request.scheduledEnd()
        )));
    }

    @Transactional
    public TaskResponse updateStatus(UUID taskId, TaskStatus status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.changeStatus(status);
        return toResponse(task);
    }

    @Transactional
    public void delete(UUID taskId) {
        taskRepository.deleteById(taskId);
    }

    private List<Task> findTasks(UUID areaId, UUID projectId, UUID goalId) {
        if (projectId != null && goalId != null) {
            return taskRepository.findAllByAreaIdAndProjectIdAndGoalIdOrderByCreatedAtDesc(areaId, projectId, goalId);
        }

        if (projectId != null) {
            return taskRepository.findAllByAreaIdAndProjectIdOrderByCreatedAtDesc(areaId, projectId);
        }

        if (goalId != null) {
            return taskRepository.findAllByAreaIdAndGoalIdOrderByCreatedAtDesc(areaId, goalId);
        }

        return taskRepository.findAllByAreaIdOrderByCreatedAtDesc(areaId);
    }

    private Project resolveProject(UUID projectId, Area area) {
        if (projectId == null) {
            return null;
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        // Cross-area links break area dashboards, so reject them before persisting the task.
        if (!project.getArea().getId().equals(area.getId())) {
            throw new IllegalArgumentException("Project does not belong to area: " + projectId);
        }

        return project;
    }

    private Goal resolveGoal(UUID goalId, Area area) {
        if (goalId == null) {
            return null;
        }

        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found: " + goalId));

        // Goal progress rolls up by area, so a task cannot point to a goal in another area.
        if (!goal.getArea().getId().equals(area.getId())) {
            throw new IllegalArgumentException("Goal does not belong to area: " + goalId);
        }

        return goal;
    }

    private TaskResponse toResponse(Task task) {
        Project project = task.getProject();
        Goal goal = task.getGoal();

        // Keep the mapping explicit so API shape changes remain obvious during review.
        return new TaskResponse(
                task.getId(),
                task.getArea().getId(),
                task.getArea().getName(),
                project == null ? null : project.getId(),
                project == null ? null : project.getName(),
                goal == null ? null : goal.getId(),
                goal == null ? null : goal.getTitle(),
                task.getTitle(),
                task.getDescription(),
                task.getTaskType(),
                task.getStatus(),
                task.getPriority(),
                task.getRecurrence(),
                task.getDueAt(),
                task.getRemindAt(),
                task.getScheduledStart(),
                task.getScheduledEnd(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
