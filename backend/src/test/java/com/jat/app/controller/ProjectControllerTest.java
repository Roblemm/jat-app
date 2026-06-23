package com.jat.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jat.app.dto.project.CreateProjectRequest;
import com.jat.app.dto.project.ProjectResponse;
import com.jat.app.service.ProjectService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void findAllReturnsProjectsForArea() throws Exception {
        UUID areaId = UUID.randomUUID();
        ProjectResponse response = new ProjectResponse(
                UUID.randomUUID(),
                areaId,
                "Career",
                "Portfolio",
                "portfolio",
                Instant.now(),
                Instant.now()
        );

        when(projectService.findAll(areaId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/projects").queryParam("areaId", areaId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Portfolio"))
                .andExpect(jsonPath("$[0].normalizedName").value("portfolio"));
    }

    @Test
    void createReturnsCreatedProject() throws Exception {
        UUID areaId = UUID.randomUUID();
        ProjectResponse response = new ProjectResponse(
                UUID.randomUUID(),
                areaId,
                "Career",
                "Portfolio",
                "portfolio",
                Instant.now(),
                Instant.now()
        );

        when(projectService.create(any(CreateProjectRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProjectRequest(areaId, "Portfolio"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Portfolio"))
                .andExpect(jsonPath("$.areaId").value(areaId.toString()));
    }

    @Test
    void createRequiresAreaId() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProjectRequest(null, "Portfolio"))))
                .andExpect(status().isBadRequest());
    }
}
