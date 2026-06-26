package com.jat.app.service;

import com.jat.app.dto.goal.CreateGoalRequest;
import com.jat.app.entity.Area;
import com.jat.app.entity.Goal;
import com.jat.app.entity.GoalType;
import com.jat.app.entity.Project;
import com.jat.app.entity.Recurrence;
import com.jat.app.repository.AreaRepository;
import com.jat.app.repository.GoalRepository;
import com.jat.app.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private AreaRepository areaRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private GoalService goalService;

    @Test
    void createAddsGoalToAreaWithoutProject() {
        // Goals can live directly under an area when a project would add unnecessary friction.
        Area area = new Area(UUID.randomUUID(), "Career", 10);
        CreateGoalRequest request = new CreateGoalRequest(
                area.getId(),
                null,
                "Complete weekly applications",
                "Keep application volume visible.",
                GoalType.TARGET,
                Recurrence.WEEKLY,
                new BigDecimal("20"),
                "applications",
                LocalDate.of(2026, 7, 3)
        );

        when(areaRepository.findById(area.getId())).thenReturn(Optional.of(area));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = goalService.create(request);

        assertThat(result.areaId()).isEqualTo(area.getId());
        assertThat(result.projectId()).isNull();
        assertThat(result.goalType()).isEqualTo(GoalType.TARGET);
        assertThat(result.recurrence()).isEqualTo(Recurrence.WEEKLY);
        assertThat(result.targetValue()).isEqualByComparingTo("20");
    }

    @Test
    void createAddsGoalToProjectWhenProjectBelongsToSameArea() {
        // Project-scoped goals keep focused work grouped without making projects mandatory everywhere.
        Area area = new Area(UUID.randomUUID(), "Career", 10);
        Project project = new Project(area, "Portfolio");
        CreateGoalRequest request = new CreateGoalRequest(
                area.getId(),
                project.getId(),
                "Ship project milestone",
                null,
                GoalType.CHECKLIST,
                Recurrence.NONE,
                null,
                null,
                null
        );

        when(areaRepository.findById(area.getId())).thenReturn(Optional.of(area));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = goalService.create(request);

        assertThat(result.projectId()).isEqualTo(project.getId());
        assertThat(result.projectName()).isEqualTo("Portfolio");
    }

    @Test
    void createRejectsProjectFromDifferentArea() {
        // Prevent cross-area links so filters and progress views stay trustworthy.
        Area career = new Area(UUID.randomUUID(), "Career", 10);
        Area business = new Area(UUID.randomUUID(), "Business", 40);
        Project project = new Project(business, "Product Research");
        CreateGoalRequest request = new CreateGoalRequest(
                career.getId(),
                project.getId(),
                "Evaluate launch examples",
                null,
                GoalType.CHECKLIST,
                Recurrence.NONE,
                null,
                null,
                null
        );

        when(areaRepository.findById(career.getId())).thenReturn(Optional.of(career));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> goalService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project does not belong to area");
    }

    @Test
    void findAllReturnsGoalsForArea() {
        // The main goals list starts area-scoped and can be narrowed by project later.
        Area area = new Area(UUID.randomUUID(), "Career", 10);
        Goal goal = new Goal(
                area,
                null,
                "Complete weekly applications",
                null,
                GoalType.TARGET,
                Recurrence.WEEKLY,
                new BigDecimal("20"),
                "applications",
                null
        );

        when(goalRepository.findAllByAreaIdOrderByCreatedAtDesc(area.getId())).thenReturn(List.of(goal));

        var result = goalService.findAll(area.getId(), null);

        assertThat(result).extracting("title").containsExactly("Complete weekly applications");
    }
}
