package com.jat.app.controller;

import com.jat.app.dto.task.CreateTaskRequest;
import com.jat.app.dto.task.TaskResponse;
import com.jat.app.service.TaskService;
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
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    // Keep HTTP concerns here and cross-entity validation inside TaskService.
    private final TaskService taskService;

    // Area is the required context; project and goal are optional filters for focused views.
    @GetMapping
    public List<TaskResponse> findAll(
            @RequestParam UUID areaId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID goalId
    ) {
        return taskService.findAll(areaId, projectId, goalId);
    }

    // Creation captures the task plus optional reminder/scheduling metadata in one round trip.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(request);
    }
}
