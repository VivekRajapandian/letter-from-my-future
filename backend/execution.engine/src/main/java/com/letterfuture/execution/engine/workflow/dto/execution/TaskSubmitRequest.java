package com.letterfuture.execution.engine.workflow.dto.execution;

import java.util.List;
import java.util.UUID;

public record TaskSubmitRequest(
        UUID userId,
        String action,
        String note,
        List<TaskSubmitValueDto> values
) {
}