package com.letterfuture.execution.engine.workflow.repository;

import com.letterfuture.execution.engine.workflow.domain.TaskResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskResponseRepository extends JpaRepository<TaskResponse, UUID> {
    List<TaskResponse> findByTaskId(UUID taskId);
    List<TaskResponse> findByQuestionId(UUID questionId);
}
