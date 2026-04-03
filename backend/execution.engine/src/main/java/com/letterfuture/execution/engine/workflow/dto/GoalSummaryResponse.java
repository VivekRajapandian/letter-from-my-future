package com.letterfuture.execution.engine.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class GoalSummaryResponse {
    UUID goalId;
    String title;
    String status;
    LocalDate targetDate;
    long completedTasks;
    long totalTasks;
    int progressPercent;
    NextTaskResponse nextTask;
    LocalDateTime updatedAt;
}
