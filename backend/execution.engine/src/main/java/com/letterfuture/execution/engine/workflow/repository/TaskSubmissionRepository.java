package com.letterfuture.execution.engine.workflow.repository;

import com.letterfuture.execution.engine.workflow.domain.TaskSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskSubmissionRepository extends JpaRepository<TaskSubmission, UUID> {

    List<TaskSubmission> findByTaskIdIn(Collection<UUID> taskIds);

    List<TaskSubmission> findByTaskIdInAndUserId(Collection<UUID> taskIds, UUID userId);

    Optional<TaskSubmission> findTopByTaskIdOrderBySubmittedAtDesc(UUID taskId);

    Optional<TaskSubmission> findTopByTaskIdAndUserIdOrderBySubmittedAtDesc(UUID taskId, UUID userId);

}