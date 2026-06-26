package com.jat.app.controller;

import com.jat.app.dto.dashboard.TodayDashboardResponse;
import com.jat.app.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    // The dashboard controller exposes screen-shaped read endpoints and leaves composition to the service.
    private final DashboardService dashboardService;

    // The date is explicit so the frontend can request today, tomorrow, or a reviewed past day with one endpoint.
    @GetMapping("/today")
    public TodayDashboardResponse getToday(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID areaId
    ) {
        return dashboardService.getToday(date, areaId);
    }
}
