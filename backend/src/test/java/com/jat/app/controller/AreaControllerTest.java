package com.jat.app.controller;

import com.jat.app.dto.area.AreaResponse;
import com.jat.app.service.AreaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AreaController.class)
class AreaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AreaService areaService;

    @Test
    void findAllReturnsAreas() throws Exception {
        // The frontend uses this endpoint to populate required area choices.
        when(areaService.findAll()).thenReturn(List.of(
                new AreaResponse(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Career", 10),
                new AreaResponse(UUID.fromString("00000000-0000-0000-0000-000000000002"), "Personal", 20)
        ));

        mockMvc.perform(get("/api/areas"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Career"))
                .andExpect(jsonPath("$[1].name").value("Personal"));
    }
}
