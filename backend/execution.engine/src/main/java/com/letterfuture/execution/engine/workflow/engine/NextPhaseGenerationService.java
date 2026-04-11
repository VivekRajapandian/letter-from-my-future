package com.letterfuture.execution.engine.workflow.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letterfuture.execution.engine.enums.GoalStatus;
import com.letterfuture.execution.engine.workflow.compiler.PlanCompiler;
import com.letterfuture.execution.engine.workflow.domain.Goal;
import com.letterfuture.execution.engine.workflow.domain.Phase;
import com.letterfuture.execution.engine.workflow.llm.OpenAiPlanClient;
import com.letterfuture.execution.engine.workflow.planning.PlanningPromptFactory;
import com.letterfuture.execution.engine.workflow.repository.GoalRepository;
import com.letterfuture.execution.engine.workflow.repository.PhaseRepository;
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
    private final PlanningPromptFactory planningPromptFactory;
    private final PlanCompiler planCompiler;
    private final ObjectMapper objectMapper;
    private final GoalRepository goalRepo;
    private final PhaseRepository phaseRepo;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String generateNextPhaseForCompletedPhase(
            Phase completedPhase,
            String progressSummary
    ) {
        Goal goal = goalRepo.findById(completedPhase.getGoalId())
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        String priorPhaseSummary = buildPriorPhasesSummary(
                completedPhase.getGoalId(),
                completedPhase.getOrderIndex()
        );

        int currentPhaseNumber = completedPhase.getOrderIndex() + 1;
        int nextPhaseNumber = currentPhaseNumber + 1;
        long totalPhases = phaseRepo.countByGoalId(completedPhase.getGoalId());

        log.info(
                "Generating next phase for goal '{}' (id: {}) - Phase {}/{} completed: '{}'",
                goal.getTitle(),
                goal.getId(),
                currentPhaseNumber,
                totalPhases,
                completedPhase.getTitle()
        );

        PlanningPromptFactory.NextPhasePromptRequest request =
                new PlanningPromptFactory.NextPhasePromptRequest(
                        goal.getTitle(),
                        nextPhaseNumber,
                        (int) totalPhases,
                        priorPhaseSummary,
                        progressSummary
                );

        String payload = planningPromptFactory.buildNextPhasePayload(request);
        return openAiPlanClient.executeResponsesPayload(payload);
    }

    @Transactional
    public UUID processNextPhaseResponse(UUID goalId, String nextPhaseJson) {
        try {
            log.debug("Processing next phase response for goal {}: {}", goalId, nextPhaseJson);

            JsonNode response = objectMapper.readTree(nextPhaseJson);
            boolean isComplete = response.path("complete").asBoolean(false);

            if (isComplete) {
                log.info("LLM determined that goal {} is complete. Marking as COMPLETED.", goalId);
                markGoalAsCompleted(goalId);
                return null;
            }

            if (response.has("phase")) {
                String phaseJson = objectMapper.writeValueAsString(response.get("phase"));
                log.debug("Extracted phase JSON for goal {}: {}", goalId, phaseJson);

                UUID nextPhaseId = planCompiler.compileAndAppendNextPhase(goalId, phaseJson);

                log.info(
                        "Successfully generated and persisted next phase {} for goal {}.",
                        nextPhaseId,
                        goalId
                );

                return nextPhaseId;
            }

            log.error("Response missing 'phase' field. Response: {}", nextPhaseJson);
            throw new IllegalArgumentException("Expected phase field in response when complete=false");
        } catch (Exception e) {
            log.error(
                    "Failed to process next phase response for goal {}: {} - Response: {}",
                    goalId,
                    e.getMessage(),
                    nextPhaseJson,
                    e
            );
            throw new RuntimeException("Invalid next phase response format: " + e.getMessage(), e);
        }
    }

    private String buildPriorPhasesSummary(UUID goalId, int completedPhaseIndex) {
        List<Phase> phases = phaseRepo.findByGoalIdOrderByOrderIndex(goalId);

        return phases.stream()
                .filter(p -> p.getOrderIndex() <= completedPhaseIndex)
                .map(p -> String.format(
                        "%d. %s (%s days)",
                        p.getOrderIndex() + 1,
                        p.getTitle(),
                        p.getDurationDays()
                ))
                .collect(Collectors.joining("\n"));
    }

    private void markGoalAsCompleted(UUID goalId) {
        Goal goal = goalRepo.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        if (goal.getStatus() != GoalStatus.COMPLETED) {
            goal.setStatus(GoalStatus.COMPLETED);
            goalRepo.save(goal);
            log.info("Goal {} marked as COMPLETED by LLM decision.", goalId);
        }
    }
}