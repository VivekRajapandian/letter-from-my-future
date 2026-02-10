package com.letterfuture.execution.engine.workflow.repository;

import com.letterfuture.execution.engine.workflow.domain.PlanVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlanVersionRepository
        extends JpaRepository<PlanVersion, UUID> {}

