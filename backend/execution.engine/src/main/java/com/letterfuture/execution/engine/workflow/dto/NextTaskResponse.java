package com.letterfuture.execution.engine.workflow.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class NextTaskResponse {

    private UUID taskId;
    private String title;
    private String description;

    private String goalTitle;

    private String phaseName;
    private int phaseIndex;
    private int phaseCount;

    private int taskIndex;
    private int taskCount;
    private int completedCount;

    private List<TaskQuestionDTO> questions;
    private List<TaskResponseDTO> responses;

    // getters
}