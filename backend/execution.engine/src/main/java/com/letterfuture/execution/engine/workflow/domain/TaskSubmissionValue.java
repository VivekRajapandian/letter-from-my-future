package com.letterfuture.execution.engine.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "task_submission_value")
@Getter
@Setter
public class TaskSubmissionValue {
    @Id
    private UUID id;
    private UUID submissionId;
    private UUID inputDefinitionId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String valueText;
    BigDecimal valueNumber;
    Boolean valueBoolean;
    @CreationTimestamp
    private LocalDate valueDate;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String valueJson;
}
