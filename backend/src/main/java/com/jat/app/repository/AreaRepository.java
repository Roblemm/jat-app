package com.jat.app.repository;

import com.jat.app.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AreaRepository extends JpaRepository<Area, UUID> {

    // Spring Data derives this query from the method name and keeps ordering close to the data access boundary.
    List<Area> findAllByOrderByDisplayOrderAscNameAsc();
}
