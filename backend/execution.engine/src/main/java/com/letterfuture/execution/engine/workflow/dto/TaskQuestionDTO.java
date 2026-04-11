package com.letterfuture.execution.engine.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class TaskQuestionDTO {
    private UUID questionId;
    private Integer questionIndex;
    private String question;
    private String questionType;
    private String hint;
}
