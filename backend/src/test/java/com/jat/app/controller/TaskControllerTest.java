package com.jat.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jat.app.dto.task.CreateTaskRequest;
import com.jat.app.dto.task.TaskResponse;
import com.jat.app.entity.Recurrence;
import com.jat.app.entity.TaskPriority;
import com.jat.app.entity.TaskStatus;
import com.jat.app.entity.TaskType;
import com.jat.app.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @Test
    void findAllReturnsTasksForArea() throws Exception {
        // The task list starts with area context and can be narrowed by project or goal query params.
        UUID areaId = UUID.randomUUID();
        TaskResponse response = new TaskResponse(
                UUID.randomUUID(),
                areaId,
                "Career",
                null,
                null,
                null,
                null,
                "Prepare weekly plan",
                null,
                TaskType.ACTION,
                TaskStatus.TODO,
                TaskPriority.HIGH,
                Recurrence.NONE,
                Instant.parse("2026-07-01T15:00:00Z"),
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(taskService.findAll(areaId, null, null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/tasks").queryParam("areaId", areaId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].title").value("Prepare weekly plan"))
                .andExpect(jsonPath("$[0].taskType").value("ACTION"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"));
    }

    @Test
    void createReturnsCreatedTask() throws Exception {
        // POST creates a captured task with optional planning fields in one request.
        UUID areaId = UUID.randomUUID();
        TaskResponse response = new TaskResponse(
                UUID.randomUUID(),
                areaId,
                "Career",
                null,
                null,
                null,
                null,
                "Prepare weekly plan",
                "Choose the highest impact work.",
                TaskType.ACTION,
                TaskStatus.TODO,
                TaskPriority.HIGH,
                Recurrence.NONE,
                Instant.parse("2026-07-01T15:00:00Z"),
                Instant.parse("2026-07-01T14:30:00Z"),
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(taskService.create(any(CreateTaskRequest.class))).thenReturn(response);

        CreateTaskRequest request = new CreateTaskRequest(
                areaId,
                null,
                null,
                "Prepare weekly plan",
                "Choose the highest impact work.",
                TaskType.ACTION,
                TaskPriority.HIGH,
                Recurrence.NONE,
                Instant.parse("2026-07-01T15:00:00Z"),
                Instant.parse("2026-07-01T14:30:00Z"),
                null,
                null
        );

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.areaId").value(areaId.toString()))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.dueAt").value("2026-07-01T15:00:00Z"));
    }

    @Test
    void createRequiresAreaId() throws Exception {
        // Area is required because tasks are intentionally not global loose notes.
        CreateTaskRequest request = new CreateTaskRequest(
                null,
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

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
