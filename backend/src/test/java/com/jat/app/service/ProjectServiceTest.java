package com.jat.app.service;

import com.jat.app.dto.project.CreateProjectRequest;
import com.jat.app.entity.Area;
import com.jat.app.entity.Project;
import com.jat.app.repository.AreaRepository;
import com.jat.app.repository.ProjectRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AreaRepository areaRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createAddsProjectToExistingArea() {
        // Creation keeps the display name while deriving a normalized value for duplicate checks.
        Area area = new Area(UUID.randomUUID(), "Career", 10);
        CreateProjectRequest request = new CreateProjectRequest(area.getId(), "Portfolio");

        when(areaRepository.findById(area.getId())).thenReturn(Optional.of(area));
        when(projectRepository.existsByAreaIdAndNormalizedName(area.getId(), "portfolio")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = projectService.create(request);

        assertThat(result.name()).isEqualTo("Portfolio");
        assertThat(result.areaId()).isEqualTo(area.getId());
        assertThat(result.normalizedName()).isEqualTo("portfolio");
    }

    @Test
    void createRejectsDuplicateNameWithinSameAreaIgnoringCase() {
        // "Portfolio" and "portfolio" should not become two autocomplete options in the same area.
        UUID areaId = UUID.randomUUID();
        CreateProjectRequest request = new CreateProjectRequest(areaId, "portfolio");

        when(areaRepository.findById(areaId)).thenReturn(Optional.of(new Area(areaId, "Career", 10)));
        when(projectRepository.existsByAreaIdAndNormalizedName(areaId, "portfolio")).thenReturn(true);

        assertThatThrownBy(() -> projectService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project already exists");
    }

    @Test
    void findByAreaReturnsProjectsForArea() {
        // Projects are intentionally queried after an area is known.
        Area area = new Area(UUID.randomUUID(), "Career", 10);
        Project portfolio = new Project(area, "Portfolio");

        when(projectRepository.findAllByAreaIdOrderByNameAsc(area.getId())).thenReturn(List.of(portfolio));

        var result = projectService.findAll(area.getId());

        assertThat(result).extracting("name").containsExactly("Portfolio");
    }
}
