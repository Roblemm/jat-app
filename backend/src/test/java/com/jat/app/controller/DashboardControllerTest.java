package com.jat.app.controller;

import com.jat.app.dto.dashboard.DashboardCounts;
import com.jat.app.dto.dashboard.TodayDashboardResponse;
import com.jat.app.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void getTodayReturnsDashboardForDate() throws Exception {
        // The endpoint gives the frontend one screen-shaped payload instead of several separate API calls.
        LocalDate date = LocalDate.of(2026, 7, 1);
        TodayDashboardResponse response = new TodayDashboardResponse(
                date,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new DashboardCounts(2, 1, 3, 4)
        );

        when(dashboardService.getToday(date, null)).thenReturn(response);

        mockMvc.perform(get("/api/dashboard/today").queryParam("date", "2026-07-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.date").value("2026-07-01"))
                .andExpect(jsonPath("$.counts.dueToday").value(2))
                .andExpect(jsonPath("$.counts.overdue").value(1))
                .andExpect(jsonPath("$.counts.scheduled").value(3))
                .andExpect(jsonPath("$.counts.activeGoals").value(4));
    }

    @Test
    void getTodayAcceptsOptionalAreaFilter() throws Exception {
        // Area filtering lets the same endpoint support both all-context and focused dashboard views.
        LocalDate date = LocalDate.of(2026, 7, 1);
        UUID areaId = UUID.randomUUID();
        TodayDashboardResponse response = new TodayDashboardResponse(
                date,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new DashboardCounts(0, 0, 0, 0)
        );

        when(dashboardService.getToday(date, areaId)).thenReturn(response);

        mockMvc.perform(get("/api/dashboard/today")
                        .queryParam("date", "2026-07-01")
                        .queryParam("areaId", areaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-07-01"));
    }
}
