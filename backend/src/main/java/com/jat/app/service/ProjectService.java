package com.jat.app.service;

import com.jat.app.dto.project.CreateProjectRequest;
import com.jat.app.dto.project.ProjectResponse;
import com.jat.app.entity.Area;
import com.jat.app.entity.Project;
import com.jat.app.repository.AreaRepository;
import com.jat.app.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final AreaRepository areaRepository;

    @Transactional(readOnly = true)
    public List<ProjectResponse> findAll(UUID areaId) {
        // The UI asks for projects after an area is selected, so the service keeps the query area-scoped.
        return projectRepository.findAllByAreaIdOrderByNameAsc(areaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        // Validate the required parent area before creating a project that depends on it.
        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new IllegalArgumentException("Area not found: " + request.areaId()));

        // Match the database unique constraint so duplicate errors are predictable and user-friendly.
        String normalizedName = Project.normalizeName(request.name());
        if (projectRepository.existsByAreaIdAndNormalizedName(area.getId(), normalizedName)) {
            throw new IllegalArgumentException("Project already exists in this area: " + request.name());
        }

        return toResponse(projectRepository.save(new Project(area, request.name())));
    }

    private ProjectResponse toResponse(Project project) {
        // Keep mapping in the service for now; extract a mapper when multiple project views appear.
        return new ProjectResponse(
                project.getId(),
                project.getArea().getId(),
                project.getArea().getName(),
                project.getName(),
                project.getNormalizedName(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
