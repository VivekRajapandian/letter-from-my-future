package com.letterfuture.execution.engine.workflow.controller;

import com.letterfuture.execution.engine.workflow.compiler.PlanCompiler;
import com.letterfuture.execution.engine.workflow.compiler.StubPlanProvider;
import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.workflow.dto.NextTaskResponse;
import com.letterfuture.execution.engine.workflow.engine.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/goals")
public class GoalController {

    private final PlanCompiler compiler;
    private final WorkflowEngine engine;
    private final StubPlanProvider stubPlanProvider;

    // Create goal from LLM plan
    @PostMapping
    public UUID createGoal(
            @RequestParam UUID userId){
        String stubJson = stubPlanProvider.getStubPlan();

        return compiler.compileAndCreateGoal(userId, stubJson);
    }

    @GetMapping("/{goalId}/next-task")
    public NextTaskResponse getNextTask(
            @PathVariable UUID goalId,
            @RequestParam UUID userId){

        return engine.getNextTask(goalId, userId);
    }

    @PostMapping("/{goalId}/{userId}/pause")
    public void pause(@PathVariable UUID goalId,
                      @PathVariable UUID userId){
        engine.pauseGoal(goalId, userId);
    }
}

