package com.letterfuture.execution.engine.workflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "task_questions")
@Getter
@Setter
public class TaskQuestion {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID taskId;

    @Column(nullable = false)
    private Integer questionIndex; // 0-3 for 4 questions max

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "question_type")
    private String questionType; // e.g., "text", "number", "date"

    private String hint;
}
