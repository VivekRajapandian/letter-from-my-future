package com.letterfuture.execution.engine.workflow.execution;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letterfuture.execution.engine.workflow.domain.Goal;
import com.letterfuture.execution.engine.workflow.domain.Phase;
import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.workflow.domain.TaskInputDefinition;
import com.letterfuture.execution.engine.workflow.domain.TaskSubmission;
import com.letterfuture.execution.engine.workflow.domain.TaskSubmissionValue;
import com.letterfuture.execution.engine.workflow.dto.execution.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ExecutionSnapshotAssembler {

    private final ObjectMapper objectMapper;

    public ExecutionSnapshotResponse toResponse(
            Goal goal,
            List<Phase> phases,
            Phase activePhase,
            List<Task> tasks,
            Map<UUID, List<TaskInputDefinition>> inputDefinitionsByTaskId,
            Map<UUID, TaskSubmission> latestSubmissionByTaskId,
            Map<UUID, List<TaskSubmissionValue>> submissionValuesBySubmissionId
    ) {
        return new ExecutionSnapshotResponse(
                toGoalDto(goal, phases),
                toPlanningDto(goal, activePhase, tasks),
                toActivePhaseDto(activePhase),
                toTaskDtos(tasks, inputDefinitionsByTaskId, latestSubmissionByTaskId, submissionValuesBySubmissionId),
                toProgressDto(phases, tasks)
        );
    }

    private ExecutionGoalDto toGoalDto(Goal goal, List<Phase> phases) {
        int phaseCountCreated = phases == null ? 0 : phases.size();

        Integer phaseCountPlanned = goal.getTotalPhasesPlanned() != null
                ? goal.getTotalPhasesPlanned()
                : phaseCountCreated;

        return new ExecutionGoalDto(
                goal.getId(),
                goal.getTitle(),
                goal.getSummary(),
                goal.getStatus(),
                goal.getPlanningMode(),
                goal.getTargetDurationDays(),
                phaseCountPlanned,
                phaseCountCreated
        );
    }

    private ExecutionPlanningDto toPlanningDto(Goal goal, Phase activePhase, List<Task> tasks) {
        String planningState = goal.getPlanningState() != null
                ? goal.getPlanningState()
                : derivePlanningState(goal, activePhase, tasks);

        boolean canGenerateNextPhase = false;
        String reason = derivePlanningReason(planningState, activePhase, tasks);

        return new ExecutionPlanningDto(
                planningState,
                canGenerateNextPhase,
                reason
        );
    }

    private String derivePlanningState(Goal goal, Phase activePhase, List<Task> tasks) {
        if (equalsIgnoreCase(goal.getStatus(), "COMPLETED")) {
            return "COMPLETED";
        }

        if (activePhase == null) {
            return "GENERATING_NEXT_PHASE";
        }

        if (tasks == null || tasks.isEmpty()) {
            return "WAITING_FOR_USER_INPUT";
        }

        return "READY";
    }

    private String derivePlanningReason(String planningState, Phase activePhase, List<Task> tasks) {
        if ("COMPLETED".equalsIgnoreCase(planningState)) {
            return null;
        }

        if ("GENERATING_NEXT_PHASE".equalsIgnoreCase(planningState)) {
            return "No active phase is currently available.";
        }

        if ("WAITING_FOR_USER_INPUT".equalsIgnoreCase(planningState)) {
            return (activePhase != null && (tasks == null || tasks.isEmpty()))
                    ? "Active phase exists but no visible tasks are available yet."
                    : "Waiting for user input.";
        }

        return null;
    }

    private ExecutionPhaseDto toActivePhaseDto(Phase activePhase) {
        if (activePhase == null) {
            return null;
        }

        return new ExecutionPhaseDto(
                activePhase.getId(),
                activePhase.getTitle(),
                activephase.getStatus().toString(),
                activePhase.getOrderIndex(),
                activePhase.getDurationDays(),
                activePhase.getTitle()
        );
    }

    private List<ExecutionTaskDto> toTaskDtos(
            List<Task> tasks,
            Map<UUID, List<TaskInputDefinition>> inputDefinitionsByTaskId,
            Map<UUID, TaskSubmission> latestSubmissionByTaskId,
            Map<UUID, List<TaskSubmissionValue>> submissionValuesBySubmissionId
    ) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }

        return tasks.stream()
                .map(task -> toTaskDto(
                        task,
                        inputDefinitionsByTaskId.getOrDefault(task.getId(), List.of()),
                        latestSubmissionByTaskId.get(task.getId()),
                        latestSubmissionByTaskId.get(task.getId()) == null
                                ? List.of()
                                : submissionValuesBySubmissionId.getOrDefault(
                                latestSubmissionByTaskId.get(task.getId()).getId(),
                                List.of()
                        )
                ))
                .toList();
    }

    private ExecutionTaskDto toTaskDto(
            Task task,
            List<TaskInputDefinition> inputDefinitions,
            TaskSubmission latestSubmission,
            List<TaskSubmissionValue> submissionValues
    ) {
        return new ExecutionTaskDto(
                task.getId(),
                task.getTitle(),
                task.getStatus().toString(),
                task.getOrderIndex(),
                task.getScheduledDay(),
                new TaskInstructionDto(
                        task.getInstructionWhat(),
                        task.getInstructionHow(),
                        task.getInstructionWhy(),
                        task.getSuccessCriteria()
                ),
                toInputSchemaDtos(inputDefinitions),
                toLatestSubmissionDto(latestSubmission, inputDefinitions, submissionValues)
        );
    }

    private List<TaskInputDefinitionDto> toInputSchemaDtos(List<TaskInputDefinition> inputDefinitions) {
        if (inputDefinitions == null || inputDefinitions.isEmpty()) {
            return List.of();
        }

        return inputDefinitions.stream()
                .map(this::toInputDefinitionDto)
                .toList();
    }

    private TaskInputDefinitionDto toInputDefinitionDto(TaskInputDefinition input) {
        return new TaskInputDefinitionDto(
                input.getId(),
                input.getKey(),
                input.getLabel(),
                input.getFieldType(),
                input.getRequired(),
                input.getPlaceholder(),
                input.getHelpText(),
                input.getUnit(),
                parseSelectOptions(input.getOptionsJson())
        );
    }

    private List<SelectOptionDto> parseSelectOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    optionsJson,
                    new TypeReference<List<SelectOptionDto>>() {}
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    private TaskLatestSubmissionDto toLatestSubmissionDto(
            TaskSubmission latestSubmission,
            List<TaskInputDefinition> inputDefinitions,
            List<TaskSubmissionValue> submissionValues
    ) {
        if (latestSubmission == null) {
            return null;
        }

        Map<UUID, TaskInputDefinition> inputDefinitionById = new HashMap<>();
        for (TaskInputDefinition definition : inputDefinitions) {
            inputDefinitionById.put(definition.getId(), definition);
        }

        Map<String, Object> valuesByInputKey = new LinkedHashMap<>();
        for (TaskSubmissionValue submissionValue : submissionValues) {
            TaskInputDefinition definition = inputDefinitionById.get(submissionValue.getInputDefinitionId());
            if (definition == null) {
                continue;
            }

            valuesByInputKey.put(
                    definition.getKey(),
                    extractValue(submissionValue)
            );
        }

        return new TaskLatestSubmissionDto(
                latestSubmission.getId(),
                latestSubmission.getAction(),
                latestSubmission.getSubmittedAt(),
                latestSubmission.getNote(),
                valuesByInputKey
        );
    }

    private Object extractValue(TaskSubmissionValue value) {
        if (value.getValueNumber() != null) {
            BigDecimal number = value.getValueNumber();
            return number.stripTrailingZeros().scale() <= 0
                    ? number.intValue()
                    : number.doubleValue();
        }

        if (value.getValueBoolean() != null) {
            return value.getValueBoolean();
        }

        if (value.getValueDate() != null) {
            LocalDate date = value.getValueDate();
            return date.toString();
        }

        if (value.getValueText() != null) {
            return value.getValueText();
        }

        if (value.getValueJson() != null && !value.getValueJson().isBlank()) {
            try {
                return objectMapper.readValue(value.getValueJson(), Object.class);
            } catch (Exception e) {
                return value.getValueJson();
            }
        }

        return null;
    }

    private ExecutionProgressDto toProgressDto(List<Phase> phases, List<Task> tasks) {
        int totalVisibleTasks = tasks == null ? 0 : tasks.size();
        int completedTasks = tasks == null ? 0 : (int) tasks.stream()
                .filter(this::isCompletedTask)
                .count();

        int phaseProgressPercent = totalVisibleTasks == 0
                ? 0
                : (completedTasks * 100) / totalVisibleTasks;

        int totalPhases = phases == null ? 0 : phases.size();
        int completedPhases = phases == null ? 0 : (int) phases.stream()
                .filter(this::isCompletedPhase)
                .count();

        int goalProgressPercent;
        if (totalPhases == 0) {
            goalProgressPercent = phaseProgressPercent;
        } else {
            goalProgressPercent = (completedPhases * 100) / totalPhases;
            if (goalProgressPercent < 100 && phaseProgressPercent > 0 && completedPhases < totalPhases) {
                int perPhaseWeight = 100 / totalPhases;
                goalProgressPercent += phaseProgressPercent / Math.max(perPhaseWeight, 1);
                if (goalProgressPercent > 99) {
                    goalProgressPercent = 99;
                }
            }
        }

        return new ExecutionProgressDto(
                completedTasks,
                totalVisibleTasks,
                phaseProgressPercent,
                goalProgressPercent
        );
    }

    private boolean isCompletedTask(Task task) {
        return equalsIgnoreCase(task.getStatus().toString(), "COMPLETED")
                || equalsIgnoreCase(task.getStatus().toString(), "DONE");
    }

    private boolean isCompletedPhase(Phase phase) {
        return equalsIgnoreCase(phase.getStatus().toString(), "COMPLETED")
                || equalsIgnoreCase(phase.getStatus().toString(), "DONE");
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && left.equalsIgnoreCase(right);
    }
}