package com.letterfuture.execution.engine.workflow.signals;

import com.letterfuture.execution.engine.enums.TaskStatus;
import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.workflow.domain.TaskInputDefinition;
import com.letterfuture.execution.engine.workflow.domain.TaskSubmission;
import com.letterfuture.execution.engine.workflow.domain.TaskSubmissionValue;
import com.letterfuture.execution.engine.workflow.repository.TaskInputDefinitionRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskSubmissionRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskSubmissionValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionAggregationService {

    private final TaskRepository taskRepository;
    private final TaskInputDefinitionRepository taskInputDefinitionRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final TaskSubmissionValueRepository taskSubmissionValueRepository;

    public PhaseExecutionSignals aggregatePhaseSignals(UUID goalId, UUID phaseId, UUID userId) {
        List<Task> tasks = taskRepository.findByPhaseIdOrderByOrderIndexAsc(phaseId);

        if (tasks.isEmpty()) {
            return new PhaseExecutionSignals(
                    goalId,
                    phaseId,
                    userId,
                    0,
                    0,
                    0,
                    0,
                    0,
                    "Phase has no tasks.",
                    List.of()
            );
        }

        List<UUID> taskIds = tasks.stream()
                .map(Task::getId)
                .toList();

        Map<UUID, List<TaskInputDefinition>> definitionsByTaskId =
                loadInputDefinitionsByTaskId(taskIds);

        Map<UUID, TaskSubmission> latestSubmissionByTaskId =
                loadLatestSubmissionByTaskId(taskIds, userId);

        Map<UUID, List<TaskSubmissionValue>> valuesBySubmissionId =
                loadSubmissionValuesBySubmissionId(latestSubmissionByTaskId.values());

        List<TaskSignalSummary> taskSignals = tasks.stream()
                .map(task -> {
                    TaskSubmission latestSubmission = latestSubmissionByTaskId.get(task.getId());

                    List<TaskInputDefinition> definitions =
                            definitionsByTaskId.getOrDefault(task.getId(), List.of());

                    List<TaskSubmissionValue> values =
                            latestSubmission == null
                                    ? List.of()
                                    : valuesBySubmissionId.getOrDefault(latestSubmission.getId(), List.of());

                    return toTaskSignalSummary(task, latestSubmission, definitions, values);
                })
                .toList();

        int completedTasks = (int) tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .count();

        int skippedTasks = (int) tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.SKIPPED)
                .count();

        int submissionCount = latestSubmissionByTaskId.size();

        int tasksWithSignals = (int) taskSignals.stream()
                .filter(signal -> !signal.capturedInputs().isEmpty()
                        || (signal.latestNote() != null && !signal.latestNote().isBlank()))
                .count();

        String progressSummary = buildProgressSummary(tasks, taskSignals);

        return new PhaseExecutionSignals(
                goalId,
                phaseId,
                userId,
                tasks.size(),
                completedTasks,
                skippedTasks,
                submissionCount,
                tasksWithSignals,
                progressSummary,
                taskSignals
        );
    }

    private Map<UUID, List<TaskInputDefinition>> loadInputDefinitionsByTaskId(List<UUID> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }

        List<TaskInputDefinition> definitions = taskInputDefinitionRepository.findByTaskIdIn(taskIds);

        return definitions.stream()
                .collect(Collectors.groupingBy(TaskInputDefinition::getTaskId));
    }

    private Map<UUID, TaskSubmission> loadLatestSubmissionByTaskId(List<UUID> taskIds, UUID userId) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }

        List<TaskSubmission> submissions = taskSubmissionRepository.findByTaskIdInAndUserId(taskIds, userId);

        Map<UUID, TaskSubmission> latestByTaskId = new HashMap<>();

        for (TaskSubmission submission : submissions) {
            TaskSubmission current = latestByTaskId.get(submission.getTaskId());

            if (current == null || isLater(submission, current)) {
                latestByTaskId.put(submission.getTaskId(), submission);
            }
        }

        return latestByTaskId;
    }

    private Map<UUID, List<TaskSubmissionValue>> loadSubmissionValuesBySubmissionId(
            Collection<TaskSubmission> submissions
    ) {
        if (submissions == null || submissions.isEmpty()) {
            return Map.of();
        }

        List<UUID> submissionIds = submissions.stream()
                .map(TaskSubmission::getId)
                .toList();

        List<TaskSubmissionValue> values =
                taskSubmissionValueRepository.findBySubmissionIdIn(submissionIds);

        return values.stream()
                .collect(Collectors.groupingBy(TaskSubmissionValue::getSubmissionId));
    }

    private boolean isLater(TaskSubmission candidate, TaskSubmission current) {
        if (candidate.getSubmittedAt() == null) {
            return false;
        }
        if (current.getSubmittedAt() == null) {
            return true;
        }
        return candidate.getSubmittedAt().isAfter(current.getSubmittedAt());
    }

    private TaskSignalSummary toTaskSignalSummary(
            Task task,
            TaskSubmission latestSubmission,
            List<TaskInputDefinition> definitions,
            List<TaskSubmissionValue> values
    ) {
        Map<UUID, TaskInputDefinition> definitionById = definitions.stream()
                .collect(Collectors.toMap(
                        TaskInputDefinition::getId,
                        definition -> definition,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Map<String, String> capturedInputs = new LinkedHashMap<>();

        values.stream()
                .sorted(Comparator.comparing(value -> {
                    TaskInputDefinition definition = definitionById.get(value.getInputDefinitionId());
                    return definition == null ? Integer.MAX_VALUE : definition.getOrderIndex();
                }))
                .forEach(value -> {
                    TaskInputDefinition definition = definitionById.get(value.getInputDefinitionId());
                    String key = definition != null && definition.getKey() != null && !definition.getKey().isBlank()
                            ? definition.getKey()
                            : String.valueOf(value.getInputDefinitionId());

                    capturedInputs.put(key, formatValue(value));
                });

        return new TaskSignalSummary(
                task.getId(),
                task.getTitle(),
                task.getStatus() == null ? "UNKNOWN" : task.getStatus().name(),
                latestSubmission == null ? null : latestSubmission.getAction(),
                latestSubmission == null ? null : latestSubmission.getNote(),
                capturedInputs
        );
    }

    private String buildProgressSummary(List<Task> tasks, List<TaskSignalSummary> taskSignals) {
        long terminalTasks = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.SKIPPED)
                .count();

        StringBuilder summary = new StringBuilder();
        summary.append("Phase progress: ")
                .append(terminalTasks)
                .append(" of ")
                .append(tasks.size())
                .append(" tasks are terminal.\n");

        for (TaskSignalSummary signal : taskSignals) {
            summary.append("Task: ")
                    .append(signal.taskTitle())
                    .append(" [")
                    .append(signal.taskStatus())
                    .append("]\n");

            if (signal.latestAction() != null) {
                summary.append(" Action: ").append(signal.latestAction()).append("\n");
            }

            if (signal.latestNote() != null && !signal.latestNote().isBlank()) {
                summary.append(" Note: ").append(signal.latestNote()).append("\n");
            }

            if (!signal.capturedInputs().isEmpty()) {
                signal.capturedInputs().forEach((key, value) ->
                        summary.append(" ").append(key).append(": ").append(value).append("\n"));
            }

            summary.append("\n");
        }

        return summary.toString().trim();
    }

    private String formatValue(TaskSubmissionValue value) {
        if (value.getValueText() != null && !value.getValueText().isBlank()) {
            return value.getValueText();
        }

        if (value.getValueNumber() != null) {
            BigDecimal normalized = value.getValueNumber().stripTrailingZeros();
            return normalized.toPlainString();
        }

        if (value.getValueBoolean() != null) {
            return String.valueOf(value.getValueBoolean());
        }

        if (value.getValueDate() != null) {
            return value.getValueDate().toString();
        }

        if (value.getValueJson() != null && !value.getValueJson().isBlank()) {
            return value.getValueJson();
        }

        return "(empty)";
    }
}