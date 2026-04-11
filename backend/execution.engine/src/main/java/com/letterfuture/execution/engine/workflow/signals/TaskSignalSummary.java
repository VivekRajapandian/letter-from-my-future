package com.letterfuture.execution.engine.workflow.signals;

import java.util.Map;
import java.util.UUID;

public record TaskSignalSummary(
        UUID taskId,
        String taskTitle,
        String taskStatus,
        String latestAction,
        String latestNote,
        Map<String, String> capturedInputs
) {
}