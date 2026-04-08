package com.letterfuture.execution.engine.workflow.engine;

import com.letterfuture.execution.engine.workflow.compiler.PlanCompiler;
import com.letterfuture.execution.engine.workflow.domain.Goal;
import com.letterfuture.execution.engine.workflow.domain.Phase;
import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.workflow.llm.OpenAiPlanClient;
import com.letterfuture.execution.engine.workflow.repository.GoalRepository;
import com.letterfuture.execution.engine.workflow.repository.PhaseRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NextPhaseGenerationService {

    private static final Logger log = LoggerFactory.getLogger(NextPhaseGenerationService.class);

    private final OpenAiPlanClient openAiPlanClient;
    private final PlanCompiler planCompiler;
    private final GoalRepository goalRepo;
    private final PhaseRepository phaseRepo;
    private final TaskRepository taskRepo;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UUID generateNextPhaseForCompletedPhase(Phase completedPhase) {
        Goal goal = goalRepo.findById(completedPhase.getGoalId())
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        log.info("Generating next phase for goal '{}' (id: {}) after phase '{}' completion.",
                goal.getTitle(), goal.getId(), completedPhase.getTitle());

        String priorPhaseSummary = buildPriorPhasesSummary(completedPhase.getGoalId(), completedPhase.getOrderIndex());
        String progressSummary = buildPhaseProgressSummary(completedPhase.getId());

        String nextPhaseJson = openAiPlanClient.generateNextPhasePlan(
                goal.getTitle(),
                priorPhaseSummary,
                progressSummary);

        UUID nextPhaseId = planCompiler.compileAndAppendNextPhase(completedPhase.getGoalId(), nextPhaseJson);

        log.info("Successfully generated and persisted next phase {} for goal {}.",
                nextPhaseId, goal.getId());

        return nextPhaseId;
    }

    private String buildPriorPhasesSummary(UUID goalId, int completedPhaseIndex) {
        List<Phase> phases = phaseRepo.findByGoalIdOrderByOrderIndex(goalId);
        return phases.stream()
                .filter(p -> p.getOrderIndex() <= completedPhaseIndex)
                .map(p -> String.format("%d. %s (%s days)", p.getOrderIndex() + 1, p.getTitle(), p.getDurationDays()))
                .collect(Collectors.joining("\n"));
    }

    private String buildPhaseProgressSummary(UUID phaseId) {
        List<Task> tasks = taskRepo.findAllByPhaseId(phaseId);
        long completed = tasks.stream().filter(t -> t.getStatus() == com.letterfuture.execution.engine.enums.TaskStatus.COMPLETED ||
                t.getStatus() == com.letterfuture.execution.engine.enums.TaskStatus.SKIPPED).count();
        return String.format("Phase progress: %d of %d tasks completed.\nCompleted tasks: %d. Remaining locked or available tasks: %d.",
                completed,
                tasks.size(),
                completed,
                tasks.size() - completed);
    }
}
