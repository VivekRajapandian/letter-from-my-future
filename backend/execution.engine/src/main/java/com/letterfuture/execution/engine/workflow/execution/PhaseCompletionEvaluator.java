package com.letterfuture.execution.engine.workflow.execution;

import com.letterfuture.execution.engine.enums.GoalStatus;
import com.letterfuture.execution.engine.enums.PhaseStatus;
import com.letterfuture.execution.engine.enums.TaskStatus;
import com.letterfuture.execution.engine.workflow.domain.Goal;
import com.letterfuture.execution.engine.workflow.domain.Phase;
import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.workflow.repository.GoalRepository;
import com.letterfuture.execution.engine.workflow.repository.PhaseRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PhaseCompletionEvaluator {

    private final TaskRepository taskRepository;
    private final PhaseRepository phaseRepository;
    private final GoalRepository goalRepository;

    @Transactional
    public PhaseEvaluationResult evaluate(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getPhaseId() == null) {
            return new PhaseEvaluationResult(
                    false,
                    false,
                    false,
                    false,
                    null,
                    null,
                    "Task has no phase."
            );
        }

        Phase currentPhase = phaseRepository.findById(task.getPhaseId())
                .orElseThrow(() -> new IllegalArgumentException("Phase not found: " + task.getPhaseId()));

        Goal goal = goalRepository.findById(currentPhase.getGoalId())
                .orElseThrow(() -> new IllegalArgumentException("Goal not found: " + currentPhase.getGoalId()));

        List<Task> phaseTasks = taskRepository.findByPhaseIdOrderByOrderIndexAsc(currentPhase.getId());

        boolean phaseCompletedNow = false;
        boolean goalCompletedNow = false;
        boolean planningStateChanged = false;
        boolean nextPhaseReady = false;
        String message;

        if (phaseTasks.isEmpty()) {
            message = "Phase has no tasks.";
            return new PhaseEvaluationResult(
                    false,
                    false,
                    false,
                    false,
                    currentPhase.getId(),
                    goal.getId(),
                    message
            );
        }

        boolean allTasksTerminal = phaseTasks.stream().allMatch(this::isTerminalTaskStatus);

        if (!allTasksTerminal) {
            markPhaseActiveIfNeeded(currentPhase);
            updateGoalForActiveExecution(goal, currentPhase);
            message = "Phase still has active tasks.";

            return new PhaseEvaluationResult(
                    false,
                    false,
                    false,
                    false,
                    currentPhase.getId(),
                    goal.getId(),
                    message
            );
        }

        if (currentPhase.getStatus() != PhaseStatus.COMPLETED) {
            currentPhase.setStatus(PhaseStatus.COMPLETED);
            setPhaseCompletedAtIfSupported(currentPhase);
            phaseRepository.save(currentPhase);
            phaseCompletedNow = true;
        }

        List<Phase> allPhases = phaseRepository.findByGoalIdOrderByOrderIndexAsc(goal.getId());

        Phase nextIncompletePhase = allPhases.stream()
                .filter(phase -> !phase.getId().equals(currentPhase.getId()))
                .filter(phase -> phase.getStatus() != PhaseStatus.COMPLETED)
                .min(Comparator.comparing(
                        Phase::getOrderIndex,
                        Comparator.nullsLast(Integer::compareTo)
                ))
                .orElse(null);

        if (nextIncompletePhase != null) {
            List<Task> nextPhaseTasks =
                    taskRepository.findByPhaseIdOrderByOrderIndexAsc(nextIncompletePhase.getId());

            boolean placeholderPhase = nextPhaseTasks.isEmpty()
                    || nextIncompletePhase.getDurationDays() == null
                    || nextIncompletePhase.getDurationDays() == 0;

            if (placeholderPhase) {
                goal.setPlanningState("GENERATING_NEXT_PHASE");
                goalRepository.save(goal);

                planningStateChanged = true;
                nextPhaseReady = false;
                message = "Current phase completed. Generating next phase tasks.";
            } else {
                if (nextIncompletePhase.getStatus() != PhaseStatus.CURRENT) {
                    nextIncompletePhase.setStatus(PhaseStatus.CURRENT);
                    phaseRepository.save(nextIncompletePhase);
                }

                goal.setPlanningState("READY");
                updateCurrentPhaseNumber(goal, nextIncompletePhase);
                goalRepository.save(goal);

                planningStateChanged = true;
                nextPhaseReady = true;
                message = "Current phase completed. Existing next phase is ready.";
            }
        } else {
            if (shouldMarkGoalCompleted(goal, allPhases)) {
                goal.setStatus(GoalStatus.COMPLETED);
                goal.setPlanningState("COMPLETED");
                goalRepository.save(goal);

                goalCompletedNow = true;
                planningStateChanged = true;
                message = "Goal completed.";
            } else {
                goal.setPlanningState("GENERATING_NEXT_PHASE");
                goalRepository.save(goal);

                planningStateChanged = true;
                message = "Current phase completed. Goal is ready for next phase generation.";
            }
        }

        return new PhaseEvaluationResult(
                phaseCompletedNow,
                goalCompletedNow,
                planningStateChanged,
                nextPhaseReady,
                currentPhase.getId(),
                goal.getId(),
                message
        );
    }

    private void markPhaseActiveIfNeeded(Phase phase) {
        if (phase.getStatus() != PhaseStatus.CURRENT) {
            phase.setStatus(PhaseStatus.CURRENT);
            phaseRepository.save(phase);
        }
    }

    private void updateGoalForActiveExecution(Goal goal, Phase currentPhase) {
        boolean changed = false;

        if (!equalsIgnoreCase(goal.getPlanningState(), "READY")) {
            goal.setPlanningState("READY");
            changed = true;
        }

        if (goal.getStatus() != GoalStatus.ACTIVE) {
            goal.setStatus(GoalStatus.ACTIVE);
            changed = true;
        }

        Integer phaseNumber = currentPhase.getPhaseNumber();
        if (phaseNumber != null && !phaseNumber.equals(goal.getCurrentPhaseNumber())) {
            goal.setCurrentPhaseNumber(phaseNumber);
            changed = true;
        }

        if (changed) {
            goalRepository.save(goal);
        }
    }

    private boolean shouldMarkGoalCompleted(Goal goal, List<Phase> allPhases) {
        if (goal.getTotalPhasesPlanned() != null && goal.getCurrentPhaseNumber() != null) {
            return goal.getCurrentPhaseNumber() >= goal.getTotalPhasesPlanned();
        }

        return allPhases.stream().allMatch(phase -> phase.getStatus() == PhaseStatus.COMPLETED);
    }

    private void updateCurrentPhaseNumber(Goal goal, Phase phase) {
        Integer phaseNumber = phase.getPhaseNumber();
        if (phaseNumber != null) {
            goal.setCurrentPhaseNumber(phaseNumber);
        }
    }

    private boolean isTerminalTaskStatus(Task task) {
        return task.getStatus() == TaskStatus.COMPLETED
                || task.getStatus() == TaskStatus.SKIPPED;
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && left.equalsIgnoreCase(right);
    }

    /**
     * Keep this method isolated because your Phase entity may or may not already have completedAt.
     * If it does, uncomment the setter call below.
     */
    private void setPhaseCompletedAtIfSupported(Phase phase) {
        try {
            var method = Phase.class.getMethod("setCompletedAt", Instant.class);
            method.invoke(phase, Instant.now());
        } catch (Exception ignored) {
            // Phase entity may not have completedAt yet. Safe to ignore for now.
        }
    }

    public record PhaseEvaluationResult(
            boolean phaseCompletedNow,
            boolean goalCompletedNow,
            boolean planningStateChanged,
            boolean nextPhaseReady,
            UUID phaseId,
            UUID goalId,
            String message
    ) {
    }
}
