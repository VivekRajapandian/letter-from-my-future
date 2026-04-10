package com.letterfuture.execution.engine.workflow.repository;

import com.letterfuture.execution.engine.workflow.domain.TaskSubmissionValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskSubmissionValueRepository extends JpaRepository<TaskSubmissionValue, UUID> {
    List<TaskSubmissionValue> findBySubmissionIdIn(Collection<UUID> submissionIds);
}