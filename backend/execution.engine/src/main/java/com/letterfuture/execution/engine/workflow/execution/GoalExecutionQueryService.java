package com.letterfuture.execution.engine.workflow.execution;

import com.letterfuture.execution.engine.workflow.domain.Goal;
import com.letterfuture.execution.engine.workflow.domain.Phase;
import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.workflow.domain.TaskInputDefinition;
import com.letterfuture.execution.engine.workflow.domain.TaskSubmission;
import com.letterfuture.execution.engine.workflow.domain.TaskSubmissionValue;
import com.letterfuture.execution.engine.workflow.dto.execution.ExecutionSnapshotResponse;
import com.letterfuture.execution.engine.workflow.repository.GoalRepository;
import com.letterfuture.execution.engine.workflow.repository.PhaseRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskInputDefinitionRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskSubmissionRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskSubmissionValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class GoalExecutionQueryService {

    private final GoalRepository goalRepository;
    private final PhaseRepository phaseRepository;
    private final TaskRepository taskRepository;
    private final TaskInputDefinitionRepository taskInputDefinitionRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final TaskSubmissionValueRepository taskSubmissionValueRepository;
    private final ExecutionSnapshotAssembler executionSnapshotAssembler;

    public ExecutionSnapshotResponse getExecutionSnapshot(UUID goalId, UUID userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Goal not found: " + goalId
                ));

        validateGoalOwnership(goal, userId);

        List<Phase> phases = phaseRepository.findByGoalIdOrderByOrderIndexAsc(goalId);
        Phase activePhase = resolveActivePhase(phases);

        List<Task> tasks = activePhase == null
                ? List.of()
                : taskRepository.findByPhaseIdOrderByOrderIndexAsc(activePhase.getId());

        List<UUID> taskIds = tasks.stream()
                .map(Task::getId)
                .toList();

        Map<UUID, List<TaskInputDefinition>> inputDefinitionsByTaskId =
                loadInputDefinitionsByTaskId(taskIds);

        Map<UUID, TaskSubmission> latestSubmissionByTaskId =
                loadLatestSubmissionByTaskId(taskIds, userId);

        Map<UUID, List<TaskSubmissionValue>> submissionValuesBySubmissionId =
                loadSubmissionValuesBySubmissionId(latestSubmissionByTaskId.values());

        return executionSnapshotAssembler.toResponse(
                goal,
                phases,
                activePhase,
                tasks,
                inputDefinitionsByTaskId,
                latestSubmissionByTaskId,
                submissionValuesBySubmissionId
        );
    }

    private void validateGoalOwnership(Goal goal, UUID userId) {
        if (goal.getUserId() == null || !goal.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    FORBIDDEN,
                    "Goal does not belong to user: " + userId
            );
        }
    }

    private Map<UUID, List<TaskInputDefinition>> loadInputDefinitionsByTaskId(List<UUID> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }

        List<TaskInputDefinition> inputDefinitions =
                taskInputDefinitionRepository.findByTaskIdIn(taskIds);

        return inputDefinitions.stream()
                .collect(Collectors.groupingBy(
                        TaskInputDefinition::getTaskId,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparing(
                                                TaskInputDefinition::getOrderIndex,
                                                Comparator.nullsLast(Integer::compareTo)
                                        ))
                                        .toList()
                        )
                ));
    }

    private Map<UUID, TaskSubmission> loadLatestSubmissionByTaskId(List<UUID> taskIds, UUID userId) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }

        List<TaskSubmission> submissions =
                taskSubmissionRepository.findByTaskIdInAndUserId(taskIds, userId);

        Map<UUID, TaskSubmission> latestByTaskId = new HashMap<>();

        for (TaskSubmission submission : submissions) {
            TaskSubmission existing = latestByTaskId.get(submission.getTaskId());
            if (existing == null || isLater(submission, existing)) {
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

    private Phase resolveActivePhase(List<Phase> phases) {
        if (phases == null || phases.isEmpty()) {
            return null;
        }

        return phases.stream()
                .filter(this::isActivePhase)
                .min(Comparator.comparing(
                        Phase::getOrderIndex,
                        Comparator.nullsLast(Integer::compareTo)
                ))
                .orElseGet(() -> phases.stream()
                        .filter(this::isIncompletePhase)
                        .min(Comparator.comparing(
                                Phase::getOrderIndex,
                                Comparator.nullsLast(Integer::compareTo)
                        ))
                        .orElse(null));
    }

    private boolean isActivePhase(Phase phase) {
        return phase.getStatus() != null
                && "ACTIVE".equalsIgnoreCase(phase.getStatus().name());
    }

    private boolean isIncompletePhase(Phase phase) {
        return phase.getStatus() == null
                || !"COMPLETED".equalsIgnoreCase(phase.getStatus().name());
    }
}