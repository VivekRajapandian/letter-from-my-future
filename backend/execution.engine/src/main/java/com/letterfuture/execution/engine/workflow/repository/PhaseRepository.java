package com.letterfuture.execution.engine.workflow.repository;

import com.letterfuture.execution.engine.workflow.domain.Phase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PhaseRepository extends JpaRepository<Phase, UUID> {
    int countByGoalId(UUID goalId);

    List<Phase> findByGoalIdOrderByOrderIndex(UUID goalId);
}
