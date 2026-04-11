package com.letterfuture.execution.engine.workflow.dto.execution;

import java.util.List;

public record ExecutionSnapshotResponse(
        ExecutionGoalDto goal,
        ExecutionPlanningDto planning,
        ExecutionPhaseDto activePhase,
        List<ExecutionTaskDto> tasks,
        ExecutionProgressDto progress
) {
}
