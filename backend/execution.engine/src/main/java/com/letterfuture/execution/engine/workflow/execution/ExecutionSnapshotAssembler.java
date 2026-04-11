package com.letterfuture.execution.engine.workflow.execution;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letterfuture.execution.engine.enums.PhaseStatus;
import com.letterfuture.execution.engine.enums.TaskStatus;
import com.letterfuture.execution.engine.workflow.domain.Goal;
import com.letterfuture.execution.engine.workflow.domain.Phase;
import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.workflow.domain.TaskInputDefinition;
import com.letterfuture.execution.engine.workflow.domain.TaskSubmission;
import com.letterfuture.execution.engine.workflow.domain.TaskSubmissionValue;
import com.letterfuture.execution.engine.workflow.dto.execution.ExecutionGoalDto;
import com.letterfuture.execution.engine.workflow.dto.execution.ExecutionPhaseDto;
import com.letterfuture.execution.engine.workflow.dto.execution.ExecutionPlanningDto;
import com.letterfuture.execution.engine.workflow.dto.execution.ExecutionProgressDto;
import com.letterfuture.execution.engine.workflow.dto.execution.ExecutionSnapshotResponse;
import com.letterfuture.execution.engine.workflow.dto.execution.ExecutionTaskDto;
import com.letterfuture.execution.engine.workflow.dto.execution.TaskInputDefinitionDto;
import com.letterfuture.execution.engine.workflow.dto.execution.TaskInstructionDto;
import com.letterfuture.execution.engine.workflow.dto.execution.TaskLatestSubmissionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        ExecutionGoalDto goalDto = new ExecutionGoalDto(
                goal.getId(),
                goal.getTitle(),
                goal.getSummary(),
                goal.getStatus() == null ? "UNKNOWN" : goal.getStatus().name(),
                goal.getPlanningMode() == null ? "UNKNOWN" : goal.getPlanningMode(),
                goal.getTargetDurationDays(),
                goal.getPhaseCountPlanned(),
                phases == null ? 0 : phases.size()
        );

        ExecutionPlanningDto planningDto = toPlanningDto(goal, phases, activePhase);
        ExecutionPhaseDto activePhaseDto = activePhase == null ? null : toPhaseDto(activePhase);

        List<ExecutionTaskDto> taskDtos = tasks == null
                ? List.of()
                : tasks.stream()
                .sorted(Comparator.comparing(Task::getOrderIndex, Comparator.nullsLast(Integer::compareTo)))
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

        ExecutionProgressDto progressDto = toProgressDto(phases, activePhase, tasks);

        return new ExecutionSnapshotResponse(
                goalDto,
                planningDto,
                activePhaseDto,
                taskDtos,
                progressDto
        );
    }

    private ExecutionPlanningDto toPlanningDto(Goal goal, List<Phase> phases, Phase activePhase) {
        String planningState = normalizePlanningState(goal.getPlanningState(), activePhase, phases);
        UUID sourcePhaseId = resolveSourcePhaseId(phases);
        UUID nextPhaseId = resolveNextPhaseId(phases, activePhase);
        LocalDateTime generatedAt = resolveGeneratedAt(nextPhaseId, phases);

        String transitionReason = buildTransitionReason(planningState, activePhase, phases);
        String futureYouMessage = buildFutureYouMessage(planningState, activePhase, phases);
        String generatedFromSignals = buildGeneratedFromSignals(planningState, activePhase, phases);

        boolean canGenerateNextPhase = "READY".equalsIgnoreCase(planningState)
                || "NEXT_PHASE_READY".equalsIgnoreCase(planningState);

        return new ExecutionPlanningDto(
                planningState,
                canGenerateNextPhase,
                transitionReason,
                sourcePhaseId,
                nextPhaseId,
                futureYouMessage,
                transitionReason,
                generatedFromSignals,
                generatedAt
        );
    }

    private ExecutionPhaseDto toPhaseDto(Phase phase) {
        return new ExecutionPhaseDto(
                phase.getId(),
                phase.getTitle(),
                phase.getStatus() == null ? "UNKNOWN" : phase.getStatus().name(),
                phase.getOrderIndex(),
                phase.getDurationDays(),
                phase.getOutlineTitle()
        );
    }

    private ExecutionTaskDto toTaskDto(
            Task task,
            List<TaskInputDefinition> inputDefinitions,
            TaskSubmission latestSubmission,
            List<TaskSubmissionValue> latestSubmissionValues
    ) {
        TaskInstructionDto instructionDto = new TaskInstructionDto(
                task.getInstructionWhat(),
                task.getInstructionHow(),
                task.getInstructionWhy(),
                task.getSuccessCriteria()
        );

        List<TaskInputDefinitionDto> inputSchema = inputDefinitions.stream()
                .sorted(Comparator.comparing(TaskInputDefinition::getOrderIndex, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toInputDefinitionDto)
                .toList();

        TaskLatestSubmissionDto latestSubmissionDto = latestSubmission == null
                ? null
                : new TaskLatestSubmissionDto(
                latestSubmission.getId(),
                latestSubmission.getAction(),
                latestSubmission.getSubmittedAt(),
                latestSubmission.getNote(),
                toSubmissionValueMap(inputDefinitions, latestSubmissionValues)
        );

        return new ExecutionTaskDto(
                task.getId(),
                task.getTitle(),
                task.getStatus() == null ? "UNKNOWN" : task.getStatus().name(),
                task.getOrderIndex(),
                task.getScheduledDay(),
                instructionDto,
                inputSchema,
                latestSubmissionDto
        );
    }

    private TaskInputDefinitionDto toInputDefinitionDto(TaskInputDefinition input) {
        return new TaskInputDefinitionDto(
                input.getId(),
                input.getKey(),
                input.getLabel(),
                input.getFieldType(),
                input.isRequired(),
                input.getPlaceholder(),
                input.getHelpText(),
                input.getUnit(),
                parseOptions(input.getOptionsJson())
        );
    }

    private Map<String, Object> toSubmissionValueMap(
            List<TaskInputDefinition> definitions,
            List<TaskSubmissionValue> values
    ) {
        Map<UUID, TaskInputDefinition> definitionById = definitions.stream()
                .collect(LinkedHashMap::new, (map, definition) -> map.put(definition.getId(), definition), Map::putAll);

        Map<String, Object> result = new LinkedHashMap<>();

        for (TaskSubmissionValue value : values) {
            TaskInputDefinition definition = definitionById.get(value.getInputDefinitionId());
            String key = definition != null && definition.getKey() != null && !definition.getKey().isBlank()
                    ? definition.getKey()
                    : String.valueOf(value.getInputDefinitionId());

            result.put(key, extractTypedValue(value));
        }

        return result;
    }

    private Object extractTypedValue(TaskSubmissionValue value) {
        if (value.getValueText() != null) {
            return value.getValueText();
        }
        if (value.getValueNumber() != null) {
            BigDecimal normalized = value.getValueNumber().stripTrailingZeros();
            return normalized.toPlainString();
        }
        if (value.getValueBoolean() != null) {
            return value.getValueBoolean();
        }
        if (value.getValueDate() != null) {
            return value.getValueDate().toString();
        }
        if (value.getValueJson() != null) {
            return value.getValueJson();
        }
        return null;
    }

    private List<Map<String, String>> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(
                    optionsJson,
                    new TypeReference<List<Map<String, String>>>() {
                    }
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private ExecutionProgressDto toProgressDto(List<Phase> phases, Phase activePhase, List<Task> tasks) {
        int completedTasks = (int) tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .count();

        int totalVisibleTasks = tasks.size();

        int phaseProgressPercent = totalVisibleTasks == 0
                ? 0
                : (completedTasks * 100) / totalVisibleTasks;

        int totalPhases = phases == null ? 0 : phases.size();
        int completedPhases = phases == null
                ? 0
                : (int) phases.stream()
                .filter(phase -> phase.getStatus() == PhaseStatus.COMPLETED)
                .count();

        int goalProgressPercent;
        if (totalPhases == 0) {
            goalProgressPercent = 0;
        } else if (activePhase == null) {
            goalProgressPercent = (completedPhases * 100) / totalPhases;
        } else {
            double weighted = completedPhases + (phaseProgressPercent / 100.0);
            goalProgressPercent = (int) Math.round((weighted * 100.0) / totalPhases);
        }

        return new ExecutionProgressDto(
                completedTasks,
                totalVisibleTasks,
                phaseProgressPercent,
                Math.min(goalProgressPercent, 100)
        );
    }

    private String normalizePlanningState(String planningState, Phase activePhase, List<Phase> phases) {
        if (planningState != null && !planningState.isBlank()) {
            return planningState.trim().toUpperCase();
        }

        boolean hasActivePhase = activePhase != null;
        boolean hasCompletedPhase = phases != null && phases.stream()
                .anyMatch(phase -> phase.getStatus() == PhaseStatus.COMPLETED);

        if (hasActivePhase) {
            return "READY";
        }

        if (hasCompletedPhase) {
            return "GENERATING_NEXT_PHASE";
        }

        return "WAITING_FOR_USER_INPUT";
    }

    private UUID resolveSourcePhaseId(List<Phase> phases) {
        if (phases == null) {
            return null;
        }

        return phases.stream()
                .filter(phase -> phase.getStatus() == PhaseStatus.COMPLETED)
                .max(Comparator.comparing(Phase::getOrderIndex, Comparator.nullsLast(Integer::compareTo)))
                .map(Phase::getId)
                .orElse(null);
    }

    private UUID resolveNextPhaseId(List<Phase> phases, Phase activePhase) {
        if (activePhase != null) {
            return activePhase.getId();
        }

        if (phases == null) {
            return null;
        }

        return phases.stream()
                .filter(phase -> phase.getStatus() != PhaseStatus.COMPLETED)
                .min(Comparator.comparing(Phase::getOrderIndex, Comparator.nullsLast(Integer::compareTo)))
                .map(Phase::getId)
                .orElse(null);
    }

    private LocalDateTime resolveGeneratedAt(UUID nextPhaseId, List<Phase> phases) {
        if (nextPhaseId == null || phases == null) {
            return null;
        }

        return phases.stream()
                .filter(phase -> nextPhaseId.equals(phase.getId()))
                .map(Phase::getCreatedAt)
                .findFirst()
                .orElse(null);
    }

    private String buildTransitionReason(String planningState, Phase activePhase, List<Phase> phases) {
        return switch (planningState.toUpperCase()) {
            case "GENERATING_NEXT_PHASE" ->
                    "The last completed phase is being translated into your next execution window.";
            case "NEXT_PHASE_READY" ->
                    "A new phase has been prepared from your latest execution signals.";
            case "COMPLETED" ->
                    "The current goal arc has been completed.";
            case "WAITING_FOR_USER_INPUT" ->
                    "The system is waiting for stronger execution signals before adapting further.";
            case "READY" -> activePhase == null
                    ? "Your workspace is available."
                    : "Your current phase is ready for focused execution.";
            default -> "Execution state is available.";
        };
    }

    private String buildFutureYouMessage(String planningState, Phase activePhase, List<Phase> phases) {
        int completedPhaseCount = countCompletedPhases(phases);

        return switch (planningState.toUpperCase()) {
            case "GENERATING_NEXT_PHASE" ->
                    "Future You is reviewing what you actually completed and shaping the next best phase.";
            case "NEXT_PHASE_READY" ->
                    completedPhaseCount > 0
                            ? "Future You adapted your next phase from the consistency you just showed."
                            : "Future You prepared the next step from your latest signals.";
            case "COMPLETED" ->
                    "Your Future You system is complete. The structure now belongs to you.";
            case "WAITING_FOR_USER_INPUT" ->
                    "Future You needs a little more execution before changing the path.";
            case "READY" ->
                    activePhase == null
                            ? "Future You has your workspace ready."
                            : "Future You left you a clear execution window. Stay with this phase.";
            default ->
                    "Future You is keeping the system aligned with your progress.";
        };
    }

    private String buildGeneratedFromSignals(String planningState, Phase activePhase, List<Phase> phases) {
        int completedPhases = countCompletedPhases(phases);
        int totalPhases = phases == null ? 0 : phases.size();

        return switch (planningState.toUpperCase()) {
            case "GENERATING_NEXT_PHASE", "NEXT_PHASE_READY" ->
                    "Generated from " + completedPhases + " completed phase"
                            + (completedPhases == 1 ? "" : "s")
                            + " across " + totalPhases + " total phase"
                            + (totalPhases == 1 ? "" : "s") + ".";
            case "COMPLETED" ->
                    "Generated from the full completed execution arc.";
            default -> null;
        };
    }

    private int countCompletedPhases(Collection<Phase> phases) {
        if (phases == null) {
            return 0;
        }

        return (int) phases.stream()
                .filter(phase -> phase.getStatus() == PhaseStatus.COMPLETED)
                .count();
    }
}