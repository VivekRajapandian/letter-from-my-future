package com.letterfuture.execution.engine.workflow.controller;

import com.letterfuture.execution.engine.workflow.engine.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
@RestController
@RequiredArgsConstructor
@RequestMapping("/tasks")
public class TaskController {

    private final WorkflowEngine engine;

    @PostMapping("/{taskId}/{userId}/complete")
    public void complete(@PathVariable UUID taskId,
                         @PathVariable UUID userId){
        engine.completeTask(taskId,userId);
    }

    @PostMapping("/{taskId}/{userId}/skip")
    public void skip(@PathVariable UUID taskId,
                     @PathVariable UUID userId){
        engine.skipTask(taskId, userId);
    }

    @PostMapping("/{taskId}/{userId}/reopen")
    public void reopen(@PathVariable UUID taskId,
                       @PathVariable UUID userId){
        engine.reopenTask(taskId, userId);
    }

    @GetMapping("/{goalId}/total-tasks")
    public long totalTasks(
            @PathVariable UUID goalId,
            @RequestParam UUID userId){

        return engine.getTotalTasks(goalId, userId);
    }

    @GetMapping("/{goalId}/completed-tasks")
    public long completedTasks(
            @PathVariable UUID goalId,
            @RequestParam UUID userId){

        return engine.getCompletedTasks(goalId, userId);
    }

    @PostMapping("/{taskId}/{userId}/resume")
    public void resume(
            @PathVariable UUID taskId,
            @PathVariable UUID userId){

        engine.resumeTask(taskId, userId);
    }
}

