package com.letterfuture.execution.engine.workflow.repository;

import com.letterfuture.execution.engine.workflow.domain.TaskQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskQuestionRepository extends JpaRepository<TaskQuestion, UUID> {
    List<TaskQuestion> findByTaskIdOrderByQuestionIndex(UUID taskId);
}
