package com.letterfuture.execution.engine.workflow.domain;

import com.letterfuture.execution.engine.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Column(nullable = false)
    private UUID phaseId;

    private String title;

    private String description;

    @Column(name = "scheduled_day")
    private Integer scheduledDay;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    private Integer orderIndex;

    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "input_data")
    private String inputData;

    @Version
    private Long version;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}

