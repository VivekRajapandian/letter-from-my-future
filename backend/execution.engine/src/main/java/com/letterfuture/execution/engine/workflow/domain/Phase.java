package com.letterfuture.execution.engine.workflow.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="phases")
@Getter
@Setter
public class Phase {

    @Id
    private UUID id;

    private UUID goalId;

    private String title;

    private Integer orderIndex;

    private LocalDateTime createdAt;
}

