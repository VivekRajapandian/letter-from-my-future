package com.letterfuture.execution.engine.workflow.dto;

import java.util.UUID;

public record NextTaskResponse(
        UUID taskId,
        String title,
        String description
) {}

