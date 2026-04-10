package com.letterfuture.execution.engine.workflow.execution;

import com.letterfuture.execution.engine.workflow.dto.execution.TaskSubmitRequest;
import com.letterfuture.execution.engine.workflow.dto.execution.TaskSubmitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v2/tasks")
@RequiredArgsConstructor
public class TaskSubmissionController {

    private final TaskSubmissionService taskSubmissionService;

    @PostMapping("/{taskId}/submit")
    public TaskSubmitResponse submitTask(
            @PathVariable UUID taskId,
            @RequestBody TaskSubmitRequest request
    ) {
        return taskSubmissionService.submit(taskId, request);
    }
}