package com.jat.app.repository;

import com.jat.app.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    // Area-scoped lists power project autocomplete after the user chooses an area.
    List<Project> findAllByAreaIdOrderByNameAsc(UUID areaId);

    // Case-insensitive uniqueness is enforced using normalizedName instead of display name.
    boolean existsByAreaIdAndNormalizedName(UUID areaId, String normalizedName);
}
