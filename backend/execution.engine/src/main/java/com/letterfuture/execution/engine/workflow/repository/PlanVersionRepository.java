package com.letterfuture.execution.engine.workflow.repository;

import com.letterfuture.execution.engine.workflow.domain.PlanVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanVersionRepository
        extends JpaRepository<PlanVersion, UUID> {

    Optional<PlanVersion> findTopByGoalIdOrderByCreatedAtDesc(UUID goalId);

    List<PlanVersion> findByGoalIdOrderByCreatedAtDesc(UUID goalId);
}

