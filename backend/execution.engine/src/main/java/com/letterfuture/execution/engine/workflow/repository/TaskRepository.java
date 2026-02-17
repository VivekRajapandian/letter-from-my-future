package com.letterfuture.execution.engine.workflow.repository;

import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    Optional<Task> findFirstByPhaseIdAndStatusOrderByOrderIndex(
            UUID phaseId,
            TaskStatus status
    );

    List<Task> findAllByPhaseId(UUID phaseId);

    @Query("""
            SELECT t
            FROM Task t
            JOIN Phase p ON t.phaseId = p.id
            WHERE p.goalId = :goalId
            AND t.status = 'AVAILABLE'
            ORDER BY p.orderIndex, t.orderIndex
            LIMIT 1
            """)
    Optional<Task> findNextTask(UUID goalId);

    List<Task> findTop3ByStatusOrderByOrderIndex(TaskStatus status);

    @Query("""
            SELECT t FROM Task t
            JOIN Phase p ON t.phaseId = p.id
            JOIN Goal g ON p.goalId = g.id
            WHERE t.id = :taskId
            AND g.userId = :userId
            """)
    Optional<Task> findByIdAndUser(UUID taskId, UUID userId);

}
