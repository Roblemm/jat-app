package com.jat.app.service;

import com.jat.app.dto.task.CreateTaskRequest;
import com.jat.app.entity.Area;
import com.jat.app.entity.Goal;
import com.jat.app.entity.GoalType;
import com.jat.app.entity.Project;
import com.jat.app.entity.Recurrence;
import com.jat.app.entity.Task;
import com.jat.app.entity.TaskPriority;
import com.jat.app.entity.TaskStatus;
import com.jat.app.entity.TaskType;
import com.jat.app.repository.AreaRepository;
import com.jat.app.repository.GoalRepository;
import com.jat.app.repository.ProjectRepository;
import com.jat.app.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AreaRepository areaRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private GoalRepository goalRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createAddsAreaOnlyTaskWithDefaultTodoStatus() {
        // Captured tasks should stay lightweight; project and goal links are useful, not mandatory.
        Area area = new Area(UUID.randomUUID(), "Career", 10);
        CreateTaskRequest request = new CreateTaskRequest(
                area.getId(),
                null,
                null,
                "Prepare weekly plan",
                "Choose the highest impact work for the week.",
                TaskType.ACTION,
                TaskPriority.HIGH,
                Recurrence.NONE,
                Instant.parse("2026-07-01T15:00:00Z"),
                Instant.parse("2026-07-01T14:30:00Z"),
                null,
                null
        );

        when(areaRepository.findById(area.getId())).thenReturn(Optional.of(area));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = taskService.create(request);

        assertThat(result.areaId()).isEqualTo(area.getId());
        assertThat(result.projectId()).isNull();
        assertThat(result.goalId()).isNull();
        assertThat(result.status()).isEqualTo(TaskStatus.TODO);
        assertThat(result.taskType()).isEqualTo(TaskType.ACTION);
        assertThat(result.priority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void createAddsTaskLinkedToProjectAndGoalInSameArea() {
        // Tasks can connect daily action to a broader goal without forcing every task into a goal.
        Area area = new Area(UUID.randomUUID(), "Career", 10);
        Project project = new Project(area, "Portfolio");
        Goal goal = new Goal(
                area,
                project,
                "Ship milestone",
                null,
                GoalType.CHECKLIST,
                Recurrence.NONE,
                null,
                null,
                null
        );
        CreateTaskRequest request = new CreateTaskRequest(
                area.getId(),
                project.getId(),
                goal.getId(),
                "Record walkthrough",
                null,
                TaskType.ACTION,
                TaskPriority.MEDIUM,
                Recurrence.NONE,
                null,
                null,
                Instant.parse("2026-07-01T18:00:00Z"),
                Instant.parse("2026-07-01T19:00:00Z")
        );

        when(areaRepository.findById(area.getId())).thenReturn(Optional.of(area));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = taskService.create(request);

        assertThat(result.projectId()).isEqualTo(project.getId());
        assertThat(result.projectName()).isEqualTo("Portfolio");
        assertThat(result.goalId()).isEqualTo(goal.getId());
        assertThat(result.goalTitle()).isEqualTo("Ship milestone");
        assertThat(result.scheduledStart()).isEqualTo(Instant.parse("2026-07-01T18:00:00Z"));
    }

    @Test
    void createRejectsProjectFromDifferentArea() {
        // Cross-area project links would make area dashboards and task filters inaccurate.
        Area career = new Area(UUID.randomUUID(), "Career", 10);
        Area personal = new Area(UUID.randomUUID(), "Personal", 20);
        Project project = new Project(personal, "Home");
        CreateTaskRequest request = new CreateTaskRequest(
                career.getId(),
                project.getId(),
                null,
                "Review checklist",
                null,
                TaskType.ACTION,
                TaskPriority.MEDIUM,
                Recurrence.NONE,
                null,
                null,
                null,
                null
        );

        when(areaRepository.findById(career.getId())).thenReturn(Optional.of(career));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> taskService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project does not belong to area");
    }

    @Test
    void createRejectsGoalFromDifferentArea() {
        // A task linked to a goal in another area would make progress rollups misleading.
        Area career = new Area(UUID.randomUUID(), "Career", 10);
        Area personal = new Area(UUID.randomUUID(), "Personal", 20);
        Goal goal = new Goal(
                personal,
                null,
                "Personal milestone",
                null,
                GoalType.CHECKLIST,
                Recurrence.NONE,
                null,
                null,
                null
        );
        CreateTaskRequest request = new CreateTaskRequest(
                career.getId(),
                null,
                goal.getId(),
                "Review milestone",
                null,
                TaskType.ACTION,
                TaskPriority.MEDIUM,
                Recurrence.NONE,
                null,
                null,
                null,
                null
        );

        when(areaRepository.findById(career.getId())).thenReturn(Optional.of(career));
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

        assertThatThrownBy(() -> taskService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Goal does not belong to area");
    }

    @Test
    void findAllReturnsAreaTasksWhenNoOptionalFilterIsProvided() {
        // The default task list is area-scoped so the UI can avoid mixing unrelated contexts.
        Area area = new Area(UUID.randomUUID(), "Career", 10);
        Task task = new Task(
                area,
                null,
                null,
                "Prepare weekly plan",
                null,
                TaskType.ACTION,
                TaskPriority.HIGH,
                Recurrence.NONE,
                null,
                null,
                null,
                null
        );

        when(taskRepository.findAllByAreaIdOrderByCreatedAtDesc(area.getId())).thenReturn(List.of(task));

        var result = taskService.findAll(area.getId(), null, null);

        assertThat(result).extracting("title").containsExactly("Prepare weekly plan");
    }

    @Test
    void updateStatusMarksTaskCompletedAndSetsCompletedAt() {
        // Completed tasks need a timestamp so later progress views can count when work was finished.
        Area area = new Area(UUID.randomUUID(), "Career", 10);
        Task task = new Task(
                area,
                null,
                null,
                "Send application",
                null,
                TaskType.ACTION,
                TaskPriority.HIGH,
                Recurrence.NONE,
                null,
                null,
                null,
                null
        );

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        var result = taskService.updateStatus(task.getId(), TaskStatus.COMPLETED);

        assertThat(result.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(result.completedAt()).isNotNull();
    }

    @Test
    void updateStatusClearsCompletedAtWhenTaskReopens() {
        // Reopening a task should remove the completion timestamp so dashboards do not double-count it.
        Area area = new Area(UUID.randomUUID(), "Career", 10);
        Task task = new Task(
                area,
                null,
                null,
                "Send application",
                null,
                TaskType.ACTION,
                TaskPriority.HIGH,
                Recurrence.NONE,
                null,
                null,
                null,
                null
        );
        task.changeStatus(TaskStatus.COMPLETED);

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        var result = taskService.updateStatus(task.getId(), TaskStatus.TODO);

        assertThat(result.status()).isEqualTo(TaskStatus.TODO);
        assertThat(result.completedAt()).isNull();
    }

    @Test
    void deleteRemovesTaskById() {
        // Hard delete is acceptable for the MVP because archived tasks remain available as a non-destructive option.
        UUID taskId = UUID.randomUUID();

        taskService.delete(taskId);

        verify(taskRepository).deleteById(taskId);
    }
}
