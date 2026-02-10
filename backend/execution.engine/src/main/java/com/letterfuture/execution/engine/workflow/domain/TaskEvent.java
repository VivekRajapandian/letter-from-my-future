package com.letterfuture.execution.engine.workflow.domain;

import com.letterfuture.execution.engine.enums.TaskEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "task_events",
        indexes = {
                @Index(name="idx_event_task", columnList="task_id"),
                @Index(name="idx_event_type", columnList="event_type")
        })
@Getter
@Setter
public class TaskEvent {

    @Id
    private UUID id;

    private UUID taskId;

    @Enumerated(EnumType.STRING)
    private TaskEventType eventType;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    private LocalDateTime createdAt;
}


