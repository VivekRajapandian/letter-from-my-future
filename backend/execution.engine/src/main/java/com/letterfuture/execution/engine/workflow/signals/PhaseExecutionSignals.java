package com.letterfuture.execution.engine.workflow.signals;

import java.util.List;
import java.util.UUID;

public record PhaseExecutionSignals(
        UUID goalId,
        UUID phaseId,
        UUID userId,
        int totalTasks,
        int completedTasks,
        int skippedTasks,
        int submissionCount,
        int tasksWithSignals,
        String progressSummary,
        List<TaskSignalSummary> taskSignals
) {
}