package com.letterfuture.execution.engine.workflow.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.workflow.domain.TaskInputDefinition;
import com.letterfuture.execution.engine.workflow.domain.TaskSubmission;
import com.letterfuture.execution.engine.workflow.domain.TaskSubmissionValue;
import com.letterfuture.execution.engine.workflow.dto.execution.TaskSubmitRequest;
import com.letterfuture.execution.engine.workflow.dto.execution.TaskSubmitResponse;
import com.letterfuture.execution.engine.workflow.dto.execution.TaskSubmitValueDto;
import com.letterfuture.execution.engine.workflow.repository.TaskInputDefinitionRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskSubmissionRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskSubmissionValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class TaskSubmissionService {

    private final TaskRepository taskRepository;
    private final TaskInputDefinitionRepository taskInputDefinitionRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final TaskSubmissionValueRepository taskSubmissionValueRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TaskSubmitResponse submit(UUID taskId, TaskSubmitRequest request) {
        validateRequest(request);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Task not found: " + taskId
                ));

        List<TaskInputDefinition> inputDefinitions =
                taskInputDefinitionRepository.findByTaskIdOrderByOrderIndexAsc(taskId);

        Map<UUID, TaskInputDefinition> inputDefinitionById = inputDefinitions.stream()
                .collect(Collectors.toMap(TaskInputDefinition::getId, Function.identity()));

        validateInputValues(request.values(), inputDefinitionById);

        TaskSubmission submission = new TaskSubmission();
        submission.setId(UUID.randomUUID());
        submission.setTaskId(taskId);
        submission.setUserId(request.userId());
        submission.setAction(normalizeAction(request.action()));
        submission.setNote(blankToNull(request.note()));
        submission.setSubmittedAt(Instant.now());

        taskSubmissionRepository.save(submission);

        List<TaskSubmissionValue> valuesToSave = buildSubmissionValues(
                submission.getId(),
                request.values(),
                inputDefinitionById
        );

        if (!valuesToSave.isEmpty()) {
            taskSubmissionValueRepository.saveAll(valuesToSave);
        }

        applyTaskStatus(task, submission.getAction());
        taskRepository.save(task);

        return new TaskSubmitResponse(
                task.getId(),
                submission.getId(),
                task.getStatus(),
                false
        );
    }

    private void validateRequest(TaskSubmitRequest request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required.");
        }

        if (request.userId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "userId is required.");
        }

        if (isBlank(request.action())) {
            throw new ResponseStatusException(BAD_REQUEST, "action is required.");
        }
    }

    private void validateInputValues(
            List<TaskSubmitValueDto> values,
            Map<UUID, TaskInputDefinition> inputDefinitionById
    ) {
        if (values == null || values.isEmpty()) {
            return;
        }

        for (TaskSubmitValueDto valueDto : values) {
            if (valueDto.inputDefinitionId() == null) {
                throw new ResponseStatusException(
                        BAD_REQUEST,
                        "Each submitted value must include inputDefinitionId."
                );
            }

            if (!inputDefinitionById.containsKey(valueDto.inputDefinitionId())) {
                throw new ResponseStatusException(
                        BAD_REQUEST,
                        "Unknown inputDefinitionId for this task: " + valueDto.inputDefinitionId()
                );
            }
        }

        List<TaskInputDefinition> requiredInputs = inputDefinitionById.values().stream()
                .filter(input -> Boolean.TRUE.equals(input.getRequired()))
                .toList();

        Map<UUID, Object> submittedByInputId = (values == null ? List.<TaskSubmitValueDto>of() : values).stream()
                .collect(Collectors.toMap(
                        TaskSubmitValueDto::inputDefinitionId,
                        TaskSubmitValueDto::value,
                        (left, right) -> right
                ));

        for (TaskInputDefinition requiredInput : requiredInputs) {
            if (!submittedByInputId.containsKey(requiredInput.getId())) {
                continue;
            }

            Object submittedValue = submittedByInputId.get(requiredInput.getId());
            if (submittedValue == null || (submittedValue instanceof String s && s.isBlank())) {
                throw new ResponseStatusException(
                        BAD_REQUEST,
                        "Required input is missing a value: " + requiredInput.getLabel()
                );
            }
        }
    }

    private List<TaskSubmissionValue> buildSubmissionValues(
            UUID submissionId,
            List<TaskSubmitValueDto> submittedValues,
            Map<UUID, TaskInputDefinition> inputDefinitionById
    ) {
        if (submittedValues == null || submittedValues.isEmpty()) {
            return List.of();
        }

        List<TaskSubmissionValue> values = new ArrayList<>();

        for (TaskSubmitValueDto dto : submittedValues) {
            TaskInputDefinition definition = inputDefinitionById.get(dto.inputDefinitionId());
            TaskSubmissionValue entity = new TaskSubmissionValue();
            entity.setId(UUID.randomUUID());
            entity.setSubmissionId(submissionId);
            entity.setInputDefinitionId(dto.inputDefinitionId());

            mapTypedValue(entity, definition, dto.value());

            values.add(entity);
        }

        return values;
    }

    private void mapTypedValue(
            TaskSubmissionValue entity,
            TaskInputDefinition definition,
            Object rawValue
    ) {
        if (rawValue == null) {
            return;
        }

        String fieldType = normalizeFieldType(definition.getFieldType());

        switch (fieldType) {
            case "NUMBER" -> entity.setValueNumber(toBigDecimal(rawValue));
            case "BOOLEAN" -> entity.setValueBoolean(toBoolean(rawValue));
            case "DATE" -> entity.setValueDate(toLocalDate(rawValue));
            case "TEXT", "MULTILINE_TEXT", "SELECT", "PHOTO", "DURATION_MINUTES", "RATING_1_TO_5" ->
                    entity.setValueText(String.valueOf(rawValue));
            default -> entity.setValueJson(toJson(rawValue));
        }
    }

    private void applyTaskStatus(Task task, String action) {
        switch (normalizeAction(action)) {
            case "SAVE_PROGRESS" -> {
                if (isBlank(task.getStatus()) || "NOT_STARTED".equalsIgnoreCase(task.getStatus())) {
                    task.setStatus("IN_PROGRESS");
                }
            }
            case "COMPLETE" -> {
                task.setStatus("COMPLETED");
                task.setCompletedAt(Instant.now());
            }
            case "SKIP" -> {
                task.setStatus("SKIPPED");
                task.setCompletedAt(null);
            }
            case "REOPEN" -> {
                task.setStatus("IN_PROGRESS");
                task.setCompletedAt(null);
            }
            default -> throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Unsupported action: " + action
            );
        }
    }

    private String normalizeAction(String action) {
        return action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeFieldType(String fieldType) {
        return fieldType == null ? "TEXT" : fieldType.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal toBigDecimal(Object rawValue) {
        try {
            if (rawValue instanceof Number number) {
                return new BigDecimal(number.toString());
            }
            return new BigDecimal(String.valueOf(rawValue).trim());
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Invalid numeric value: " + rawValue
            );
        }
    }

    private Boolean toBoolean(Object rawValue) {
        if (rawValue instanceof Boolean b) {
            return b;
        }

        String text = String.valueOf(rawValue).trim().toLowerCase(Locale.ROOT);
        if ("true".equals(text)) return true;
        if ("false".equals(text)) return false;

        throw new ResponseStatusException(
                BAD_REQUEST,
                "Invalid boolean value: " + rawValue
        );
    }

    private LocalDate toLocalDate(Object rawValue) {
        try {
            return LocalDate.parse(String.valueOf(rawValue).trim());
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Invalid date value. Expected YYYY-MM-DD: " + rawValue
            );
        }
    }

    private String toJson(Object rawValue) {
        try {
            return objectMapper.writeValueAsString(rawValue);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Unable to serialize submitted value."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}