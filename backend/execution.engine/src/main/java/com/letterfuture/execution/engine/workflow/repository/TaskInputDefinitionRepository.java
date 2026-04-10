package com.letterfuture.execution.engine.workflow.repository;

import com.letterfuture.execution.engine.workflow.domain.TaskInputDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskInputDefinitionRepository extends JpaRepository<TaskInputDefinition, UUID> {

    List<TaskInputDefinition> findByTaskIdOrderByOrderIndexAsc(UUID taskId);

    List<TaskInputDefinition> findByTaskIdIn(Collection<UUID> taskIds);
}