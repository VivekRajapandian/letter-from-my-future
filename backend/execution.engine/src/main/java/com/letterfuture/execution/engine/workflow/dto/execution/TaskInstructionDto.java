package com.letterfuture.execution.engine.workflow.dto.execution;

public record TaskInstructionDto(
        String what,
        String how,
        String why,
        String successCriteria
) {
}
