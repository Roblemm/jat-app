package com.jat.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jat.app.dto.goal.CreateGoalRequest;
import com.jat.app.dto.goal.GoalResponse;
import com.jat.app.entity.GoalStatus;
import com.jat.app.entity.GoalType;
import com.jat.app.entity.Recurrence;
import com.jat.app.service.GoalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoalController.class)
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GoalService goalService;

    @Test
    void findAllReturnsGoalsForArea() throws Exception {
        // The list endpoint is area-first, with project filtering available as a narrower view.
        UUID areaId = UUID.randomUUID();
        GoalResponse response = new GoalResponse(
                UUID.randomUUID(),
                areaId,
                "Career",
                null,
                null,
                "Complete weekly applications",
                null,
                GoalType.TARGET,
                GoalStatus.ACTIVE,
                Recurrence.WEEKLY,
                new BigDecimal("20"),
                "applications",
                LocalDate.of(2026, 7, 3),
                Instant.now(),
                Instant.now()
        );

        when(goalService.findAll(areaId, null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/goals").queryParam("areaId", areaId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].title").value("Complete weekly applications"))
                .andExpect(jsonPath("$[0].goalType").value("TARGET"))
                .andExpect(jsonPath("$[0].recurrence").value("WEEKLY"));
    }

    @Test
    void createReturnsCreatedGoal() throws Exception {
        // Goal creation accepts the measurement fields now; progress entries come in a later API slice.
        UUID areaId = UUID.randomUUID();
        GoalResponse response = new GoalResponse(
                UUID.randomUUID(),
                areaId,
                "Career",
                null,
                null,
                "Complete weekly applications",
                "Keep weekly progress visible.",
                GoalType.TARGET,
                GoalStatus.ACTIVE,
                Recurrence.WEEKLY,
                new BigDecimal("20"),
                "applications",
                LocalDate.of(2026, 7, 3),
                Instant.now(),
                Instant.now()
        );

        when(goalService.create(any(CreateGoalRequest.class))).thenReturn(response);

        CreateGoalRequest request = new CreateGoalRequest(
                areaId,
                null,
                "Complete weekly applications",
                "Keep weekly progress visible.",
                GoalType.TARGET,
                Recurrence.WEEKLY,
                new BigDecimal("20"),
                "applications",
                LocalDate.of(2026, 7, 3)
        );

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.areaId").value(areaId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.targetValue").value(20));
    }

    @Test
    void createRequiresAreaId() throws Exception {
        // A goal without an area would not appear reliably in area-based dashboards.
        CreateGoalRequest request = new CreateGoalRequest(
                null,
                null,
                "Complete weekly applications",
                null,
                GoalType.TARGET,
                Recurrence.WEEKLY,
                new BigDecimal("20"),
                "applications",
                null
        );

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
