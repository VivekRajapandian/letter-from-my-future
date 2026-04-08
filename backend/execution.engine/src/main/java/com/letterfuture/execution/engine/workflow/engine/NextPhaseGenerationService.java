package com.letterfuture.execution.engine.workflow.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letterfuture.execution.engine.enums.GoalStatus;
import com.letterfuture.execution.engine.workflow.compiler.PlanCompiler;
import com.letterfuture.execution.engine.workflow.domain.Goal;
import com.letterfuture.execution.engine.workflow.domain.Phase;
import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.workflow.domain.TaskQuestion;
import com.letterfuture.execution.engine.workflow.domain.TaskResponse;
import com.letterfuture.execution.engine.workflow.llm.OpenAiPlanClient;
import com.letterfuture.execution.engine.workflow.repository.GoalRepository;
import com.letterfuture.execution.engine.workflow.repository.PhaseRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskQuestionRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskResponseRepository;
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
    private final ObjectMapper objectMapper;
    private final GoalRepository goalRepo;
    private final PhaseRepository phaseRepo;
    private final TaskRepository taskRepo;
    private final TaskQuestionRepository taskQuestionRepo;
    private final TaskResponseRepository taskResponseRepo;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String generateNextPhaseForCompletedPhase(Phase completedPhase) {
        // Extract all data needed for LLM call before suspending transaction
        Goal goal = goalRepo.findById(completedPhase.getGoalId())
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        String priorPhaseSummary = buildPriorPhasesSummary(completedPhase.getGoalId(), completedPhase.getOrderIndex());
        String progressSummary = buildPhaseProgressSummary(completedPhase.getId());

        log.info("Generating next phase for goal '{}' (id: {}) after phase '{}' completion.",
                goal.getTitle(), goal.getId(), completedPhase.getTitle());

        // Now suspend transaction and make LLM call
        String nextPhaseJson = openAiPlanClient.generateNextPhasePlan(
                goal.getTitle(),
                priorPhaseSummary,
                progressSummary);

        return nextPhaseJson;
    }

    @Transactional
    public UUID processNextPhaseResponse(UUID goalId, String nextPhaseJson) {
        try {
            JsonNode response = objectMapper.readTree(nextPhaseJson);
            boolean isComplete = response.path("complete").asBoolean(false);

            if (isComplete) {
                log.info("LLM determined that goal {} is complete. Marking as COMPLETED.", goalId);
                markGoalAsCompleted(goalId);
                return null; // No next phase to generate
            }

            // Extract phase from response and compile it
            if (response.has("phase")) {
                String phaseJson = objectMapper.writeValueAsString(response.get("phase"));
                List<Phase> phases = phaseRepo.findByGoalIdOrderByOrderIndex(goalId);
                Phase lastPhase = phases.get(phases.size() - 1);
                UUID nextPhaseId = planCompiler.compileAndAppendNextPhase(goalId, phaseJson);
                log.info("Successfully generated and persisted next phase {} for goal {}.",
                        nextPhaseId, goalId);
                return nextPhaseId;
            } else {
                throw new IllegalArgumentException("Expected phase field in response when complete=false");
            }
        } catch (Exception e) {
            log.error("Failed to parse next phase response for goal {}: {}", goalId, e.getMessage());
            throw new RuntimeException("Invalid next phase response format", e);
        }
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
        
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Phase progress: %d of %d tasks completed.\nCompleted tasks: %d. Remaining locked or available tasks: %d.\n\n",
                completed,
                tasks.size(),
                completed,
                tasks.size() - completed));
        
        // Include user responses for completed tasks
        summary.append("User progress data:\n");
        for (Task task : tasks) {
            if (task.getStatus() == com.letterfuture.execution.engine.enums.TaskStatus.COMPLETED) {
                summary.append(String.format("Task: %s\n", task.getTitle()));
                List<TaskQuestion> questions = taskQuestionRepo.findByTaskIdOrderByQuestionIndex(task.getId());
                List<TaskResponse> responses = taskResponseRepo.findByTaskId(task.getId());
                
                if (!responses.isEmpty()) {
                    for (TaskQuestion q : questions) {
                        TaskResponse resp = responses.stream()
                                .filter(r -> r.getQuestionId().equals(q.getId()))
                                .findFirst()
                                .orElse(null);
                        if (resp != null) {
                            summary.append(String.format("  Q: %s → A: %s\n", q.getQuestion(), resp.getResponse()));
                        }
                    }
                }
            }
        }
        
        return summary.toString();
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
