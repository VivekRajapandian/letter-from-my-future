package com.letterfuture.execution.engine.workflow.repository;

import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface TaskRepository extends JpaRepository<Task, UUID> {
    Optional<Task> findFirstByPhaseIdAndStatusOrderByOrderIndex(
            UUID phaseId,
            TaskStatus status
    );

    List<Task> findAllByPhaseId(UUID phaseId);

}
