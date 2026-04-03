package com.letterfuture.execution.engine.workflow.repository;

import com.letterfuture.execution.engine.enums.GoalStatus;
import com.letterfuture.execution.engine.workflow.domain.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {
    Optional<Goal> findByIdAndUserId(UUID goalId, UUID userId);

    List<Goal> findByUserIdAndStatusInOrderByUpdatedAtDesc(UUID userId, Collection<GoalStatus> statuses);
}