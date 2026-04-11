package com.letterfuture.execution.engine.workflow.dto.execution;

import java.util.List;
import java.util.UUID;

public record TaskInputDefinitionDto(
        UUID inputDefinitionId,
        String key,
        String label,
        String type,
        Boolean required,
        String placeholder,
        String helpText,
        String unit,
        List<SelectOptionDto> options
) {
}
