package com.jat.app.controller;

import com.jat.app.dto.area.AreaResponse;
import com.jat.app.service.AreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/areas")
@RequiredArgsConstructor
public class AreaController {

    private final AreaService areaService;

    @GetMapping
    public List<AreaResponse> findAll() {
        return areaService.findAll();
    }
}
