package com.letterfuture.execution.engine.workflow.planning;

import com.letterfuture.execution.engine.workflow.domain.Goal;
import com.letterfuture.execution.engine.workflow.execution.PhaseCompletionEvaluator;
import com.letterfuture.execution.engine.workflow.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NextPhaseTriggerService {

    private final GoalRepository goalRepository;

    private final ProgressivePlanningService progressivePlanningService;

    @Transactional
    public NextPhaseTriggerResult handlePhaseEvaluation(
            PhaseCompletionEvaluator.PhaseEvaluationResult evaluationResult
    ) {
        if (evaluationResult == null) {
            return new NextPhaseTriggerResult(
                    false,
                    false,
                    null,
                    "No phase evaluation result provided."
            );
        }

        if (evaluationResult.goalId() == null) {
            return new NextPhaseTriggerResult(
                    false,
                    false,
                    null,
                    "No goalId present in phase evaluation result."
            );
        }


        Goal goal = goalRepository.findById(evaluationResult.goalId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Goal not found: " + evaluationResult.goalId()
                ));

        if (evaluationResult.goalCompletedNow() || equalsIgnoreCase(goal.getStatus().toString(), "COMPLETED")) {
            ensurePlanningState(goal, "COMPLETED");

            return new NextPhaseTriggerResult(
                    false,
                    false,
                    goal.getId(),
                    "Goal is already completed."
            );
        }

        if (evaluationResult.nextPhaseReady()) {
            ensurePlanningState(goal, "READY");

            return new NextPhaseTriggerResult(
                    false,
                    false,
                    goal.getId(),
                    "Existing next phase is already available."
            );
        }

        if (!evaluationResult.planningStateChanged()) {
            return new NextPhaseTriggerResult(
                    false,
                    false,
                    goal.getId(),
                    "No planning state change detected."
            );
        }

        if (equalsIgnoreCase(goal.getPlanningState(), "GENERATING_NEXT_PHASE")) {
            progressivePlanningService.generateNextPhase(goal.getId());

            return new NextPhaseTriggerResult(
                    true,
                    true,
                    goal.getId(),
                    "Next phase generation started."
            );
        }

        return new NextPhaseTriggerResult(
                false,
                false,
                goal.getId(),
                "No next phase generation needed."
        );
    }

    private void ensurePlanningState(Goal goal, String expectedState) {
        if (!equalsIgnoreCase(goal.getPlanningState(), expectedState)) {
            goal.setPlanningState(expectedState);
            goalRepository.save(goal);
        }
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && left.equalsIgnoreCase(right);
    }

    public record NextPhaseTriggerResult(
            boolean generationNeeded,
            boolean generationStarted,
            UUID goalId,
            String message
    ) {
    }
}