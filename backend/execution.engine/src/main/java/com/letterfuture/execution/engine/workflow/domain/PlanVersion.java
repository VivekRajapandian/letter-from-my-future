package com.letterfuture.execution.engine.workflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name="plan_versions",
        indexes = @Index(name="idx_plan_goal", columnList="goal_id"))
@Getter
@Setter
public class PlanVersion {

    @Id
    private UUID id;

    private UUID goalId;

    @Column(columnDefinition = "jsonb")
    private String planJson;

    private LocalDateTime createdAt;
}

