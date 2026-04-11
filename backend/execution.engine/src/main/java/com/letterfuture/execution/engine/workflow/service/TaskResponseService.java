package com.letterfuture.execution.engine.workflow.service;

import com.letterfuture.execution.engine.workflow.domain.TaskResponse;
import com.letterfuture.execution.engine.workflow.dto.SubmitTaskResponseRequest;
import com.letterfuture.execution.engine.workflow.repository.TaskResponseRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskResponseService {

    private static final Logger log = LoggerFactory.getLogger(TaskResponseService.class);
    private final TaskResponseRepository taskResponseRepo;

    @Transactional
    public void saveTaskResponses(UUID taskId, List<SubmitTaskResponseRequest> responses) {
        log.info("Saving {} responses for task {}", responses.size(), taskId);

        for (SubmitTaskResponseRequest request : responses) {
            TaskResponse response = new TaskResponse();
            response.setId(UUID.randomUUID());
            response.setTaskId(taskId);
            response.setQuestionId(request.getQuestionId());
            response.setResponse(request.getResponse());

            taskResponseRepo.save(response);
        }

        log.info("Successfully saved responses for task {}", taskId);
    }
}
