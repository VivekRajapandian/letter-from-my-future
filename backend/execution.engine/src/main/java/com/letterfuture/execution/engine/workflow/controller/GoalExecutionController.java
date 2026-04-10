package com.letterfuture.execution.engine.workflow.controller;

import com.letterfuture.execution.engine.workflow.dto.execution.ExecutionSnapshotResponse;
import com.letterfuture.execution.engine.workflow.execution.GoalExecutionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v2/goals")
public class GoalExecutionController {

    private final GoalExecutionQueryService goalExecutionQueryService;

    @GetMapping("/{goalId}/execution")
    public ExecutionSnapshotResponse getExecutionSnapshot(
            @PathVariable UUID goalId,
            @RequestParam UUID userId
    ) {
        return goalExecutionQueryService.getExecutionSnapshot(goalId, userId);
    }
}
