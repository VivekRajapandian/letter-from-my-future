package com.letterfuture.execution.engine.workflow.repository;

import com.letterfuture.execution.engine.workflow.domain.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {
    Optional<Goal> findByIdAndUserId(UUID goalId, UUID userId);
}