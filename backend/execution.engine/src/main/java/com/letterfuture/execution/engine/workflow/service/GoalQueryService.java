package com.letterfuture.execution.engine.workflow.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letterfuture.execution.engine.enums.GoalStatus;
import com.letterfuture.execution.engine.enums.PhaseStatus;
import com.letterfuture.execution.engine.enums.TaskStatus;
import com.letterfuture.execution.engine.workflow.domain.Goal;
import com.letterfuture.execution.engine.workflow.domain.Phase;
import com.letterfuture.execution.engine.workflow.domain.PlanVersion;
import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.workflow.domain.TaskQuestion;
import com.letterfuture.execution.engine.workflow.domain.TaskResponse;
import com.letterfuture.execution.engine.workflow.dto.GoalCardResponse;
import com.letterfuture.execution.engine.workflow.dto.GoalExecutionResponse;
import com.letterfuture.execution.engine.workflow.dto.GoalSummaryResponse;
import com.letterfuture.execution.engine.workflow.dto.NextTaskResponse;
import com.letterfuture.execution.engine.workflow.dto.OngoingGoalsResponse;
import com.letterfuture.execution.engine.workflow.engine.WorkflowEngine;
import com.letterfuture.execution.engine.workflow.repository.GoalRepository;
import com.letterfuture.execution.engine.workflow.repository.PhaseRepository;
import com.letterfuture.execution.engine.workflow.repository.PlanVersionRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskQuestionRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalQueryService {

    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;
    private final WorkflowEngine workflowEngine;
    private final PhaseRepository phaseRepository;
    private final PlanVersionRepository planVersionRepository;
    private final TaskQuestionRepository taskQuestionRepository;
    private final TaskResponseRepository taskResponseRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern INSTRUCTION_PATTERN = Pattern.compile(
            "(?is)(?:^|\\n)\\s*(what|how|why|success\\s*criteria)\\s*:\\s*(.*?)(?=\\n\\s*(?:what|how|why|success\\s*criteria)\\s*:|$)"
    );

    public OngoingGoalsResponse getOngoingGoals(UUID userId) {
        List<GoalStatus> ongoingStatuses = List.of(GoalStatus.ACTIVE, GoalStatus.PAUSED);

        List<Goal> goals = goalRepository.findByUserIdAndStatusInOrderByUpdatedAtDesc(userId, ongoingStatuses);

        List<GoalCardResponse> goalCards = goals.stream()
                .map(goal -> buildGoalCard(goal, userId))
                .toList();

        return new OngoingGoalsResponse(
                userId,
                !goalCards.isEmpty(),
                goalCards
        );
    }

    public GoalSummaryResponse getGoalSummary(UUID goalId, UUID userId) {
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        long totalTasks = taskRepository.countTotalTasks(goalId, userId);
        long completedTasks = taskRepository.countCompletedTasks(goalId, userId);
        int progressPercent = calculateProgressPercent(completedTasks, totalTasks);

        NextTaskResponse nextTask = null;
        try {
            nextTask = workflowEngine.getNextTask(goalId, userId);
        } catch (RuntimeException ignored) {
            // No next task available; keep nextTask as null
        }

        return new GoalSummaryResponse(
                goal.getId(),
                goal.getTitle(),
                goal.getStatus().name(),
                goal.getTargetDate(),
                completedTasks,
                totalTasks,
                progressPercent,
                nextTask,
                goal.getUpdatedAt()
        );
    }

    public GoalExecutionResponse getGoalExecution(UUID goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        List<Phase> phases = phaseRepository.findByGoalIdOrderByOrderIndex(goalId);
        Optional<Phase> activePhase = phases.stream()
                .filter(phase -> phase.getStatus() == PhaseStatus.CURRENT)
                .findFirst();

        PlanMetadata planMetadata = loadPlanMetadata(goalId);
        int phaseCountCreated = countCreatedPhases(phases);

        GoalExecutionResponse.GoalView goalView = new GoalExecutionResponse.GoalView(
                goal.getId(),
                goal.getTitle(),
                goal.getSummary(),
                goal.getStatus().name(),
                planMetadata.planningMode(),
                goal.getTargetDurationDays(),
                planMetadata.phaseCountPlanned(),
                phaseCountCreated
        );

        GoalExecutionResponse.ActivePhaseView activePhaseView = activePhase
                .map(phase -> buildActivePhaseView(phase, planMetadata))
                .orElse(null);

        List<Task> activePhaseTasks = activePhase
                .map(phase -> taskRepository.findAllByPhaseIdOrderByOrderIndex(phase.getId()))
                .orElseGet(List::of);
        List<Task> visibleTasks = activePhaseTasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.LOCKED)
                .toList();

        GoalExecutionResponse.PlanningView planningView = new GoalExecutionResponse.PlanningView(
                goal.getStatus() == GoalStatus.COMPLETED ? "COMPLETED" : "READY",
                canGenerateNextPhase(goal, phases, activePhaseTasks),
                null
        );

        List<GoalExecutionResponse.TaskExecutionView> taskViews = visibleTasks.stream()
                .map(this::buildTaskExecutionView)
                .toList();

        long completedVisibleTasks = visibleTasks.stream()
                .filter(this::isTerminalTask)
                .count();
        long goalCompletedTasks = taskRepository.countCompletedTasks(goalId);
        long goalTotalTasks = taskRepository.countTotalTasks(goalId);

        GoalExecutionResponse.ProgressView progressView = new GoalExecutionResponse.ProgressView(
                completedVisibleTasks,
                visibleTasks.size(),
                calculateProgressPercent(completedVisibleTasks, visibleTasks.size()),
                calculateProgressPercent(goalCompletedTasks, goalTotalTasks)
        );

        return new GoalExecutionResponse(
                goalView,
                planningView,
                activePhaseView,
                taskViews,
                progressView
        );
    }

    private GoalCardResponse buildGoalCard(Goal goal, UUID userId) {
        long totalTasks = taskRepository.countTotalTasks(goal.getId(), userId);
        long completedTasks = taskRepository.countCompletedTasks(goal.getId(), userId);
        int progressPercent = calculateProgressPercent(completedTasks, totalTasks);

        String nextTaskTitle = null;
        String phaseName = null;
        int phaseIndex = 0;
        int phaseCount = 0;

        try {
            NextTaskResponse nextTask = workflowEngine.getNextTask(goal.getId(), userId);
            nextTaskTitle = nextTask.getTitle();
            phaseName = nextTask.getPhaseName();
            phaseIndex = nextTask.getPhaseIndex();
            phaseCount = nextTask.getPhaseCount();
        } catch (RuntimeException ignored) {
            // Goal may have no currently available task
        }

        return new GoalCardResponse(
                goal.getId(),
                goal.getTitle(),
                goal.getStatus().name(),
                progressPercent,
                completedTasks,
                totalTasks,
                nextTaskTitle,
                phaseName,
                phaseIndex,
                phaseCount,
                goal.getTargetDate(),
                goal.getUpdatedAt()
        );
    }

    private int calculateProgressPercent(long completedTasks, long totalTasks) {
        if (totalTasks == 0) {
            return 0;
        }
        return (int) Math.round((completedTasks * 100.0) / totalTasks);
    }

    private GoalExecutionResponse.ActivePhaseView buildActivePhaseView(Phase phase, PlanMetadata planMetadata) {
        return new GoalExecutionResponse.ActivePhaseView(
                phase.getId(),
                phase.getTitle(),
                mapPhaseStatus(phase.getStatus()),
                phase.getOrderIndex() == null ? null : phase.getOrderIndex() + 1,
                phase.getDurationDays(),
                planMetadata.outlineTitleAt(phase.getOrderIndex())
        );
    }

    private GoalExecutionResponse.TaskExecutionView buildTaskExecutionView(Task task) {
        List<TaskQuestion> questions = taskQuestionRepository.findByTaskIdOrderByQuestionIndex(task.getId());
        Map<UUID, String> keyByQuestionId = new LinkedHashMap<>();
        List<GoalExecutionResponse.InputFieldView> inputSchema = buildInputSchema(questions, keyByQuestionId);

        return new GoalExecutionResponse.TaskExecutionView(
                task.getId(),
                task.getTitle(),
                task.getStatus().name(),
                task.getOrderIndex() == null ? null : task.getOrderIndex() + 1,
                task.getScheduledDay(),
                parseInstruction(task.getDescription()),
                inputSchema,
                buildLatestSubmission(task.getId(), questions, keyByQuestionId)
        );
    }

    private List<GoalExecutionResponse.InputFieldView> buildInputSchema(List<TaskQuestion> questions,
                                                                        Map<UUID, String> keyByQuestionId) {
        List<GoalExecutionResponse.InputFieldView> fields = new ArrayList<>();
        Map<String, Integer> keyCounts = new LinkedHashMap<>();

        for (TaskQuestion question : questions) {
            String key = uniqueKey(slugify(question.getQuestion()), keyCounts);
            keyByQuestionId.put(question.getId(), key);

            fields.add(new GoalExecutionResponse.InputFieldView(
                    question.getId(),
                    key,
                    question.getQuestion(),
                    normalizeInputType(question.getQuestionType()),
                    false,
                    question.getHint()
            ));
        }

        return fields;
    }

    private GoalExecutionResponse.SubmissionView buildLatestSubmission(UUID taskId,
                                                                       List<TaskQuestion> questions,
                                                                       Map<UUID, String> keyByQuestionId) {
        List<TaskResponse> responses = taskResponseRepository.findByTaskId(taskId);
        if (responses.isEmpty()) {
            return null;
        }

        Map<UUID, TaskResponse> latestResponseByQuestion = new LinkedHashMap<>();

        responses.stream()
                .sorted(Comparator.comparing(TaskResponse::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)))
                .forEach(response -> latestResponseByQuestion.put(response.getQuestionId(), response));

        LocalDateTime submittedAt = latestResponseByQuestion.values().stream()
                .map(TaskResponse::getCreatedAt)
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        Map<String, Object> values = new LinkedHashMap<>();
        for (TaskQuestion question : questions) {
            TaskResponse response = latestResponseByQuestion.get(question.getId());
            if (response == null) {
                continue;
            }

            String key = keyByQuestionId.get(question.getId());
            if (key == null) {
                continue;
            }

            values.put(key, coerceResponseValue(response.getResponse(), question.getQuestionType()));
        }

        if (values.isEmpty()) {
            return null;
        }

        return new GoalExecutionResponse.SubmissionView(submittedAt, values);
    }

    private GoalExecutionResponse.InstructionView parseInstruction(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        Matcher matcher = INSTRUCTION_PATTERN.matcher(description);
        Map<String, String> sections = new LinkedHashMap<>();
        while (matcher.find()) {
            sections.put(
                    matcher.group(1).toLowerCase(Locale.ROOT).replaceAll("\\s+", ""),
                    matcher.group(2).trim()
            );
        }

        if (sections.isEmpty()) {
            return new GoalExecutionResponse.InstructionView(description.trim(), null, null, null);
        }

        return new GoalExecutionResponse.InstructionView(
                sections.get("what"),
                sections.get("how"),
                sections.get("why"),
                sections.get("successcriteria")
        );
    }

    private PlanMetadata loadPlanMetadata(UUID goalId) {
        List<PlanVersion> planVersions = planVersionRepository.findByGoalIdOrderByCreatedAtDesc(goalId);
        if (planVersions.isEmpty()) {
            return new PlanMetadata("PROGRESSIVE", null, List.of());
        }

        for (PlanVersion planVersion : planVersions) {
            try {
                JsonNode root = objectMapper.readTree(planVersion.getPlanJson());
                List<String> phaseOutline = new ArrayList<>();
                JsonNode phaseOutlineNode = root.path("phaseOutline");
                if (phaseOutlineNode.isMissingNode() || phaseOutlineNode.isNull()) {
                    phaseOutlineNode = root.path("phase_outline");
                }
                if (phaseOutlineNode.isArray()) {
                    phaseOutlineNode.forEach(node -> phaseOutline.add(node.asText()));
                }

                Integer phaseCountPlanned = null;
                JsonNode totalPhasesNode = root.path("totalPhases");
                if (totalPhasesNode.isMissingNode() || totalPhasesNode.isNull()) {
                    totalPhasesNode = root.path("total_phases");
                }
                if (totalPhasesNode.canConvertToInt()) {
                    phaseCountPlanned = totalPhasesNode.asInt();
                } else if (!phaseOutline.isEmpty()) {
                    phaseCountPlanned = phaseOutline.size();
                }

                if (phaseCountPlanned != null || !phaseOutline.isEmpty()) {
                    return new PlanMetadata("PROGRESSIVE", phaseCountPlanned, phaseOutline);
                }
            } catch (Exception ex) {
                throw new RuntimeException("Failed to parse plan metadata for goal " + goalId, ex);
            }
        }

        return new PlanMetadata("STATIC", null, List.of());
    }

    private int countCreatedPhases(List<Phase> phases) {
        int createdPhases = 0;
        for (Phase phase : phases) {
            if (!taskRepository.findAllByPhaseId(phase.getId()).isEmpty()) {
                createdPhases++;
            }
        }
        return createdPhases;
    }

    private boolean canGenerateNextPhase(Goal goal, List<Phase> phases, List<Task> activePhaseTasks) {
        if (goal.getStatus() == GoalStatus.COMPLETED || activePhaseTasks.isEmpty()) {
            return false;
        }

        boolean hasPlannedPhase = phases.stream().anyMatch(phase -> phase.getStatus() == PhaseStatus.PLANNED);
        return hasPlannedPhase && activePhaseTasks.stream().allMatch(this::isTerminalTask);
    }

    private boolean isTerminalTask(Task task) {
        return task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.SKIPPED;
    }

    private String mapPhaseStatus(PhaseStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case CURRENT -> "ACTIVE";
            case PLANNED -> "PLANNED";
            case COMPLETED -> "COMPLETED";
            case PAUSED -> "PAUSED";
        };
    }

    private String normalizeInputType(String questionType) {
        if (questionType == null || questionType.isBlank()) {
            return "text";
        }

        return switch (questionType.toLowerCase(Locale.ROOT)) {
            case "text", "string" -> "text";
            case "textarea", "multiline", "multiline_text" -> "multiline_text";
            case "int", "integer", "float", "double", "decimal", "number" -> "number";
            case "bool", "boolean" -> "boolean";
            default -> questionType.toLowerCase(Locale.ROOT);
        };
    }

    private Object coerceResponseValue(String responseValue, String questionType) {
        if (responseValue == null) {
            return null;
        }

        String normalizedType = normalizeInputType(questionType);
        try {
            return switch (normalizedType) {
                case "number" -> parseNumericValue(responseValue);
                case "boolean" -> Boolean.parseBoolean(responseValue);
                default -> responseValue;
            };
        } catch (NumberFormatException ex) {
            return responseValue;
        }
    }

    private Object parseNumericValue(String value) {
        if (value.contains(".")) {
            return Double.parseDouble(value);
        }
        return Long.parseLong(value);
    }

    private String slugify(String source) {
        if (source == null || source.isBlank()) {
            return "field";
        }

        String slug = source.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        return slug.isBlank() ? "field" : slug;
    }

    private String uniqueKey(String baseKey, Map<String, Integer> keyCounts) {
        int nextCount = keyCounts.getOrDefault(baseKey, 0);
        keyCounts.put(baseKey, nextCount + 1);
        return nextCount == 0 ? baseKey : baseKey + "_" + (nextCount + 1);
    }

    private record PlanMetadata(
            String planningMode,
            Integer phaseCountPlanned,
            List<String> phaseOutline
    ) {
        private String outlineTitleAt(Integer orderIndex) {
            if (orderIndex == null || orderIndex < 0 || orderIndex >= phaseOutline.size()) {
                return null;
            }
            return phaseOutline.get(orderIndex);
        }
    }
}
