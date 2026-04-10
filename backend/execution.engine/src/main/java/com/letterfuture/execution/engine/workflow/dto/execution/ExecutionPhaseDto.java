package com.letterfuture.execution.engine.workflow.dto.execution;

import java.util.UUID;

public record ExecutionPhaseDto(
        UUID phaseId,
        String title,
        String status,
        Integer orderIndex,
        Integer durationDays,
        String outlineTitle
) {
}
