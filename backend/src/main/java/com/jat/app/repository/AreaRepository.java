package com.jat.app.repository;

import com.jat.app.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AreaRepository extends JpaRepository<Area, UUID> {

    List<Area> findAllByOrderByDisplayOrderAscNameAsc();
}
