package com.letterfuture.execution.engine.workflow.controller;

import com.letterfuture.execution.engine.workflow.compiler.PlanCompiler;
import com.letterfuture.execution.engine.workflow.dto.CreateGoalRequest;
import com.letterfuture.execution.engine.workflow.dto.NextTaskResponse;
import com.letterfuture.execution.engine.workflow.engine.WorkflowEngine;
import com.letterfuture.execution.engine.workflow.llm.OpenAiPlanClient;
import com.letterfuture.execution.engine.workflow.planning.PlanningPromptFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/goals")
public class GoalController {

    private static final int MAX_PLAN_GENERATION_RETRIES = 5;
    private static final Logger log = LoggerFactory.getLogger(GoalController.class);

    private final PlanCompiler compiler;
    private final WorkflowEngine engine;
    private final OpenAiPlanClient openAiPlanClient;
    private final PlanningPromptFactory planningPromptFactory;

    @PostMapping
    public UUID createGoal(
            @RequestParam UUID userId,
            @Valid @RequestBody CreateGoalRequest request
    ) {
        int maxAttempts = MAX_PLAN_GENERATION_RETRIES + 1;
        IllegalArgumentException lastValidationFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String payload = planningPromptFactory.buildInitialGoalPlanPayload(
                    request.getGoalDescription()
            );

            String llmResponseJson = openAiPlanClient.executeResponsesPayload(payload);

            try {
                return compiler.compileAndCreateInitialGoal(
                        userId,
                        request.getGoalDescription(),
                        llmResponseJson
                );
            } catch (IllegalArgumentException ex) {
                lastValidationFailure = ex;
                log.warn(
                        "Generated plan rejected for user {} on attempt {}/{}: {}",
                        userId,
                        attempt,
                        maxAttempts,
                        ex.getMessage()
                );
            }
        }

        throw lastValidationFailure;
    }

    @GetMapping("/{goalId}/next-task")
    public NextTaskResponse getNextTask(
            @PathVariable UUID goalId,
            @RequestParam UUID userId
    ) {
        return engine.getNextTask(goalId, userId);
    }

    @PostMapping("/{goalId}/{userId}/pause")
    public void pause(
            @PathVariable UUID goalId,
            @PathVariable UUID userId
    ) {
        engine.pauseGoal(goalId, userId);
    }
}