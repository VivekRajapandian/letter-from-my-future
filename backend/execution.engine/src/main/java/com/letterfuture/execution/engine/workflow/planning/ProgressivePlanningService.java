package com.letterfuture.execution.engine.workflow.planning;

import com.letterfuture.execution.engine.enums.GoalStatus;
import com.letterfuture.execution.engine.enums.PhaseStatus;
import com.letterfuture.execution.engine.workflow.domain.Goal;
import com.letterfuture.execution.engine.workflow.domain.Phase;
import com.letterfuture.execution.engine.workflow.engine.NextPhaseGenerationService;
import com.letterfuture.execution.engine.workflow.repository.GoalRepository;
import com.letterfuture.execution.engine.workflow.repository.PhaseRepository;
import com.letterfuture.execution.engine.workflow.signals.PhaseExecutionSignals;
import com.letterfuture.execution.engine.workflow.signals.SubmissionAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressivePlanningService {

    private final GoalRepository goalRepository;
    private final PhaseRepository phaseRepository;
    private final SubmissionAggregationService submissionAggregationService;
    private final NextPhaseGenerationService nextPhaseGenerationService;

    @Transactional
    public ProgressivePlanningResult generateNextPhase(UUID goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found: " + goalId));

        Phase completedPhase = resolveCompletedPhaseForGeneration(goalId);

        PhaseExecutionSignals signals = submissionAggregationService.aggregatePhaseSignals(
                goalId,
                completedPhase.getId(),
                goal.getUserId()
        );

        String llmResponse = nextPhaseGenerationService.generateNextPhaseForCompletedPhase(
                completedPhase,
                signals.progressSummary()
        );

        UUID generatedPhaseId = nextPhaseGenerationService.processNextPhaseResponse(goalId, llmResponse);

        Goal refreshedGoal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found after generation: " + goalId));

        if (generatedPhaseId != null) {
            updateGoalAfterNextPhaseCreated(refreshedGoal);
            return new ProgressivePlanningResult(
                    goalId,
                    completedPhase.getId(),
                    generatedPhaseId,
                    false,
                    signals,
                    "Next phase generated successfully."
            );
        }

        markGoalCompletedPlanningState(refreshedGoal);

        return new ProgressivePlanningResult(
                goalId,
                completedPhase.getId(),
                null,
                true,
                signals,
                "Goal marked complete by progressive planning."
        );
    }

    private Phase resolveCompletedPhaseForGeneration(UUID goalId) {
        List<Phase> phases = phaseRepository.findByGoalIdOrderByOrderIndexAsc(goalId);

        return phases.stream()
                .filter(phase -> phase.getStatus() == PhaseStatus.COMPLETED)
                .max(Comparator.comparing(
                        Phase::getOrderIndex,
                        Comparator.nullsLast(Integer::compareTo)
                ))
                .orElseThrow(() -> new IllegalStateException(
                        "No completed phase available for next-phase generation for goal: " + goalId
                ));
    }

    private void updateGoalAfterNextPhaseCreated(Goal goal) {
        boolean changed = false;

        if (!equalsIgnoreCase(goal.getPlanningState(), "READY")) {
            goal.setPlanningState("READY");
            changed = true;
        }

        if (goal.getStatus() != GoalStatus.ACTIVE) {
            goal.setStatus(GoalStatus.ACTIVE);
            changed = true;
        }

        if (changed) {
            goalRepository.save(goal);
        }
    }

    private void markGoalCompletedPlanningState(Goal goal) {
        boolean changed = false;

        if (!equalsIgnoreCase(goal.getPlanningState(), "COMPLETED")) {
            goal.setPlanningState("COMPLETED");
            changed = true;
        }

        if (goal.getStatus() != GoalStatus.COMPLETED) {
            goal.setStatus(GoalStatus.COMPLETED);
            changed = true;
        }

        if (changed) {
            goalRepository.save(goal);
        }
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && left.equalsIgnoreCase(right);
    }

    public record ProgressivePlanningResult(
            UUID goalId,
            UUID sourcePhaseId,
            UUID generatedPhaseId,
            boolean goalCompleted,
            PhaseExecutionSignals signals,
            String message
    ) {
    }
}