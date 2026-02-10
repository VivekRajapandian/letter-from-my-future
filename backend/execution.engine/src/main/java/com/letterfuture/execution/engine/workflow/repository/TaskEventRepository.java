package com.letterfuture.execution.engine.workflow.repository;

import com.letterfuture.execution.engine.workflow.domain.TaskEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskEventRepository extends JpaRepository<TaskEvent, UUID> {}
