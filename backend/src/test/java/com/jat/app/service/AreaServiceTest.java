package com.jat.app.service;

import com.jat.app.entity.Area;
import com.jat.app.repository.AreaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AreaServiceTest {

    @Mock
    private AreaRepository areaRepository;

    @InjectMocks
    private AreaService areaService;

    @Test
    void findAllReturnsAreasOrderedByDisplayOrder() {
        Area career = new Area(UUID.randomUUID(), "Career", 10);
        Area personal = new Area(UUID.randomUUID(), "Personal", 20);

        when(areaRepository.findAllByOrderByDisplayOrderAscNameAsc()).thenReturn(List.of(career, personal));

        var result = areaService.findAll();

        assertThat(result).extracting("name").containsExactly("Career", "Personal");
    }
}
