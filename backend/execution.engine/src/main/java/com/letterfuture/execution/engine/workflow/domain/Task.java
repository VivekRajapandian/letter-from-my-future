package com.letterfuture.execution.engine.workflow.domain;

import com.letterfuture.execution.engine.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="tasks",
        indexes = @Index(name="idx_task_status", columnList="status"))
@Getter
@Setter
public class Task {

    @Id
    private UUID id;

    private UUID phaseId;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    private Integer orderIndex;

    private LocalDateTime createdAt;
}

