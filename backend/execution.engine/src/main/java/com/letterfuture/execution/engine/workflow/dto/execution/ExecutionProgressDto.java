package com.letterfuture.execution.engine.workflow.dto.execution;

public record ExecutionProgressDto(
        Integer completedTasks,
        Integer totalVisibleTasks,
        Integer phaseProgressPercent,
        Integer goalProgressPercent
) {
}