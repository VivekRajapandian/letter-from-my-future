package com.letterfuture.execution.engine.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "task_submission")
@Getter
@Setter
public class TaskSubmission {

    @Id
    private UUID id;
    private UUID taskId;
    private UUID userId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String action; // SAVE_PROGRESS, COMPLETE, SKIP, REOPEN
    @Column(nullable = false, columnDefinition = "TEXT")
    private String note;
    @CreationTimestamp
    private LocalDateTime submittedAt;
}
