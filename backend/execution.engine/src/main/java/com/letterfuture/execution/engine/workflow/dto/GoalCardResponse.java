package com.letterfuture.execution.engine.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class GoalCardResponse {
    UUID goalId;
    String title;
    String status;
    int progressPercent;
    long completedTasks;
    long totalTasks;
    String nextTaskTitle;
    String phaseName;
    int phaseIndex;
    int phaseCount;
    LocalDate targetDate;
    LocalDateTime updatedAt;
}
