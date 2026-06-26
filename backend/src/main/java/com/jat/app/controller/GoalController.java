package com.jat.app.controller;

import com.jat.app.dto.goal.CreateGoalRequest;
import com.jat.app.dto.goal.GoalResponse;
import com.jat.app.service.GoalService;
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
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    // The controller owns HTTP shape only; GoalService owns validation that depends on stored data.
    private final GoalService goalService;

    // Goals are listed by required area context, then optionally narrowed to a project.
    @GetMapping
    public List<GoalResponse> findAll(
            @RequestParam UUID areaId,
            @RequestParam(required = false) UUID projectId
    ) {
        return goalService.findAll(areaId, projectId);
    }

    // The create endpoint captures goal setup; automatic progress calculation is added in later slices.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse create(@Valid @RequestBody CreateGoalRequest request) {
        return goalService.create(request);
    }
}
