package com.letterfuture.execution.engine.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_input_definition")
@Getter
@Setter
public class TaskInputDefinition {
    @Id
    private UUID id;
    @Id
    private UUID taskId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String key;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String label;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String fieldType; // TEXT, MULTILINE_TEXT, NUMBER, BOOLEAN, DATE, SELECT, PHOTO, DURATION_MINUTES, RATING_1_TO_5
    @Column(nullable = false, columnDefinition = "TEXT")
    private String placeholder;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String helpText;
    boolean required;
    int orderIndex;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String unit;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String optionsJson;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String validationJson;
    Instant createdAt;
    Instant updatedAt;
}
