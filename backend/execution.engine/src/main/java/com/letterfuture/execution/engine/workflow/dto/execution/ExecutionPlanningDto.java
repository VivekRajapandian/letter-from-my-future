package com.letterfuture.execution.engine.workflow.dto.execution;

public record ExecutionPlanningDto(
        String state,
        Boolean canGenerateNextPhase,
        String reason
) {
}