package com.letterfuture.execution.engine.workflow.dto.execution;

import java.util.UUID;

public record TaskSubmitValueDto(
        UUID inputDefinitionId,
        Object value
) {
}