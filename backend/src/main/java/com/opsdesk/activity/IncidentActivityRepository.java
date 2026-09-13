package com.opsdesk.activity;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentActivityRepository extends JpaRepository<IncidentActivity, Long> {
    @EntityGraph(attributePaths = "actor")
    List<IncidentActivity> findByIncidentIdOrderByCreatedAtAscIdAsc(Long incidentId);
}
