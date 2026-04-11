package com.letterfuture.execution.engine.workflow.dto.execution;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExecutionPlanningDto(
        String state,
        Boolean canGenerateNextPhase,
        String reason,
        UUID sourcePhaseId,
        UUID nextPhaseId,
        String futureYouMessage,
        String transitionReason,
        String generatedFromSignals,
        LocalDateTime generatedAt
) {
}