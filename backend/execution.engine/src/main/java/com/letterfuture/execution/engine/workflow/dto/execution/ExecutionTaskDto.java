package com.letterfuture.execution.engine.workflow.dto.execution;

import java.util.List;
import java.util.UUID;

public record ExecutionTaskDto(
        UUID taskId,
        String title,
        String status,
        Integer orderIndex,
        Integer scheduledDay,
        TaskInstructionDto instruction,
        List<TaskInputDefinitionDto> inputSchema,
        TaskLatestSubmissionDto latestSubmission
) {
}
