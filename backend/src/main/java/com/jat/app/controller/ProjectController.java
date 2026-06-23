package com.jat.app.controller;

import com.jat.app.dto.project.CreateProjectRequest;
import com.jat.app.dto.project.ProjectResponse;
import com.jat.app.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    // The controller stays thin: HTTP validation/routing here, business rules in ProjectService.
    private final ProjectService projectService;

    // Projects are listed within an area because area context is required before project autocomplete is useful.
    @GetMapping
    public List<ProjectResponse> findAll(@RequestParam UUID areaId) {
        return projectService.findAll(areaId);
    }

    // Creating a project is the backend version of the UI's free-create autocomplete behavior.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(request);
    }
}
