package com.letterfuture.execution.engine.workflow.repository;

import com.letterfuture.execution.engine.enums.PhaseStatus;
import com.letterfuture.execution.engine.workflow.domain.Phase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhaseRepository extends JpaRepository<Phase, UUID> {
    int countByGoalId(UUID goalId);

    List<Phase> findByGoalIdOrderByOrderIndex(UUID goalId);

    Optional<Phase> findByGoalIdAndOrderIndex(UUID goalId, Integer orderIndex);

    Optional<Phase> findByGoalIdAndStatus(UUID goalId, PhaseStatus status);

    Optional<Phase> findFirstByGoalIdAndStatusOrderByOrderIndex(UUID goalId, PhaseStatus status);
}
