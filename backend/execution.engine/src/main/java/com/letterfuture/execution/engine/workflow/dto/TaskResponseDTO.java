package com.letterfuture.execution.engine.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class TaskResponseDTO {
    private UUID questionId;
    private String response;
}
