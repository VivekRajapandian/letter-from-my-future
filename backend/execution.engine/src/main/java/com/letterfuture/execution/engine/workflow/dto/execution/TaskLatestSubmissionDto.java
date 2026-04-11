package com.letterfuture.execution.engine.workflow.dto.execution;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record TaskLatestSubmissionDto(
        UUID submissionId,
        String action,
        LocalDateTime submittedAt,
        String note,
        Map<String, Object> values
) {
}
