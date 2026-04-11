package com.letterfuture.execution.engine.workflow.dto.execution;

import java.util.UUID;

public record TaskSubmitResponse(
        UUID taskId,
        UUID submissionId,
        String executionState,
        boolean planningTriggered
) {
}
