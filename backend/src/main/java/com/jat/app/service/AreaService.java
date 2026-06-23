package com.jat.app.service;

import com.jat.app.dto.area.AreaResponse;
import com.jat.app.repository.AreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaRepository areaRepository;

    @Transactional(readOnly = true)
    public List<AreaResponse> findAll() {
        // Areas should appear consistently in navigation, filters, and quick-capture prompts.
        return areaRepository.findAllByOrderByDisplayOrderAscNameAsc()
                .stream()
                .map(area -> new AreaResponse(area.getId(), area.getName(), area.getDisplayOrder()))
                .toList();
    }
}
