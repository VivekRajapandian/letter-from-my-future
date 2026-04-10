package com.letterfuture.execution.engine.workflow.dto.execution;

import java.util.UUID;

public record ExecutionGoalDto(
        UUID goalId,
        String title,
        String summary,
        String status,
        String planningMode,
        Integer targetDurationDays,
        Integer phaseCountPlanned,
        Integer phaseCountCreated
) {
}
