package com.letterfuture.execution.engine.workflow.compiler;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letterfuture.execution.engine.enums.GoalStatus;
import com.letterfuture.execution.engine.enums.PhaseStatus;
import com.letterfuture.execution.engine.enums.TaskStatus;
import com.letterfuture.execution.engine.workflow.dto.InitialGoalPlanDto;
import com.letterfuture.execution.engine.workflow.dto.InitialPhaseDto;
import com.letterfuture.execution.engine.workflow.dto.PlanDto;
import com.letterfuture.execution.engine.workflow.dto.PhaseDto;
import com.letterfuture.execution.engine.workflow.dto.TaskQuestionInputDto;
import com.letterfuture.execution.engine.workflow.domain.*;
import com.letterfuture.execution.engine.workflow.repository.*;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanCompiler {

    private static final int MAX_TOTAL_TASKS = 40;

    private final ObjectMapper objectMapper;
    private final Validator validator;

    private final GoalRepository goalRepo;
    private final PhaseRepository phaseRepo;
    private final TaskRepository taskRepo;
    private final TaskQuestionRepository taskQuestionRepo;
    private final PlanVersionRepository planVersionRepo;

    /**
     * Compiles STRICT LLM JSON into a persisted plan.
     * Reject invalid JSON (no "best effort" fixing).
     */
    @Transactional
    public UUID compileAndCreateGoal(UUID userId, String rawPlanJson) {
        //Validation → Structure → Normalize → Limits.
        PlanDto dto = parseStrict(rawPlanJson);
        validate(dto);
        enforceStructuralRules(dto);
        PlanDto normalized = normalize(dto);
        enforceLimits(normalized);
        String persistedPlanJson = serializePlan(normalized);

        // 1) Goal
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID());
        goal.setUserId(userId);
        goal.setTitle(normalized.getGoalTitle());
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setCreatedAt(LocalDateTime.now());
        goalRepo.save(goal);

        // 2) PlanVersion (immutable)
        PlanVersion pv = new PlanVersion();
        pv.setId(UUID.randomUUID());
        pv.setGoalId(goal.getId());
        pv.setPlanJson(persistedPlanJson);
        pv.setCreatedAt(LocalDateTime.now());
        planVersionRepo.save(pv);

        // 3) Phases + Tasks
        int phaseIndex = 0;
        boolean firstTaskUnlocked = false;

        for (var phaseDto : normalized.getPhases()) {
            Phase phase = new Phase();
            phase.setId(UUID.randomUUID());
            phase.setGoalId(goal.getId());
            phase.setTitle(phaseDto.getTitle());
            phase.setOrderIndex(phaseIndex++);
            phase.setCreatedAt(LocalDateTime.now());
            phaseRepo.save(phase);

            int taskIndex = 0;
            for (var taskDto : phaseDto.getTasks()) {
                Task task = new Task();
                task.setId(UUID.randomUUID());
                task.setPhaseId(phase.getId());
                task.setTitle(taskDto.getTitle());
                task.setDescription(taskDto.getDescription());
                task.setOrderIndex(taskIndex++);
                task.setCreatedAt(LocalDateTime.now());

                // IMPORTANT: only first task is AVAILABLE
                if (!firstTaskUnlocked) {
                    task.setStatus(TaskStatus.AVAILABLE);
                    firstTaskUnlocked = true;
                } else {
                    task.setStatus(TaskStatus.LOCKED);
                }

                taskRepo.save(task);
            }
        }

        return goal.getId();
    }

    @Transactional
    public UUID compileAndCreateInitialGoal(UUID userId, String goalDescription, String rawPlanJson) {
        InitialGoalPlanDto dto = parseInitialPlan(rawPlanJson);
        validateInitial(dto);
        enforceInitialStructuralRules(dto);
        normalizeInitial(dto);
        enforceInitialLimits(dto);
        String persistedPlanJson = serializeInitialPlan(dto);

        Goal goal = new Goal();
        goal.setId(UUID.randomUUID());
        goal.setUserId(userId);
        goal.setTitle(dto.getGoalTitle());
        goal.setDescription(goalDescription);
        goal.setSummary(dto.getGoalSummary());
        goal.setTargetDurationDays(dto.getTargetDurationDays());
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setCreatedAt(LocalDateTime.now());
        goalRepo.save(goal);

        PlanVersion pv = new PlanVersion();
        pv.setId(UUID.randomUUID());
        pv.setGoalId(goal.getId());
        pv.setPlanJson(persistedPlanJson);
        pv.setCreatedAt(LocalDateTime.now());
        planVersionRepo.save(pv);

        // Create all phases upfront based on phase_outline
        UUID firstPhaseId = null;
        for (int i = 0; i < dto.getPhaseOutline().size(); i++) {
            Phase phase = new Phase();
            phase.setId(UUID.randomUUID());
            phase.setGoalId(goal.getId());
            phase.setTitle(dto.getPhaseOutline().get(i));
            phase.setStatus(i == 0 ? PhaseStatus.CURRENT : PhaseStatus.PLANNED);
            phase.setOrderIndex(i);
            phase.setCreatedAt(LocalDateTime.now());
            // Duration will be set when we generate the detailed phase
            phase.setDurationDays(0);
            phaseRepo.save(phase);
            
            if (i == 0) {
                firstPhaseId = phase.getId();
            }
        }

        // Now populate the first phase with tasks from the detailed phase
        Phase firstPhase = phaseRepo.findById(firstPhaseId)
                .orElseThrow(() -> new RuntimeException("First phase not found"));
        firstPhase.setDurationDays(dto.getPhase().getDurationDays());
        phaseRepo.save(firstPhase);

        boolean firstTaskUnlocked = false;
        int taskIndex = 0;
        for (var taskDto : dto.getPhase().getTasks()) {
            Task task = new Task();
            task.setId(UUID.randomUUID());
            task.setPhaseId(firstPhase.getId());
            task.setTitle(taskDto.getTitle());
            task.setDescription(taskDto.getDescription());
            task.setScheduledDay(taskDto.getDay());
            task.setOrderIndex(taskIndex++);
            task.setCreatedAt(LocalDateTime.now());
            task.setInputData(null);

            if (!firstTaskUnlocked) {
                task.setStatus(TaskStatus.AVAILABLE);
                firstTaskUnlocked = true;
            } else {
                task.setStatus(TaskStatus.LOCKED);
            }

            taskRepo.save(task);
            
            // Save task questions if provided
            saveTaskQuestions(task.getId(), taskDto.getQuestions());
        }

        return goal.getId();
    }

    @Transactional
    public UUID compileAndAppendNextPhase(UUID goalId, String rawPhaseJson) {
        PhaseDto dto = parseNextPhasePlan(rawPhaseJson);
        validateNextPhase(dto);
        enforceNextPhaseStructuralRules(dto);
        normalizeNextPhase(dto);
        enforceNextPhaseLimits(dto, goalId);
        String persistedPlanJson = serializePhase(dto);

        List<Phase> phases = phaseRepo.findByGoalIdOrderByOrderIndexAsc(goalId);

        Phase nextPhase = phases.stream()
                .filter(phase -> phase.getStatus() != PhaseStatus.COMPLETED)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Next phase not found for goal: " + goalId));

        nextPhase.setStatus(PhaseStatus.CURRENT);
        nextPhase.setDurationDays(dto.getDurationDays());
        phaseRepo.save(nextPhase);

        boolean firstTaskUnlocked = false;
        int taskIndex = 0;

        for (var taskDto : dto.getTasks()) {
            Task task = new Task();
            task.setId(UUID.randomUUID());
            task.setPhaseId(nextPhase.getId());
            task.setTitle(taskDto.getTitle());
            task.setDescription(taskDto.getDescription());
            task.setScheduledDay(taskDto.getDay());
            task.setOrderIndex(taskIndex++);
            task.setCreatedAt(LocalDateTime.now());
            task.setInputData(null);

            if (!firstTaskUnlocked) {
                task.setStatus(TaskStatus.AVAILABLE);
                firstTaskUnlocked = true;
            } else {
                task.setStatus(TaskStatus.LOCKED);
            }

            taskRepo.save(task);
            saveTaskQuestions(task.getId(), taskDto.getQuestions());
        }

        PlanVersion pv = new PlanVersion();
        pv.setId(UUID.randomUUID());
        pv.setGoalId(goalId);
        pv.setPlanJson(persistedPlanJson);
        pv.setCreatedAt(LocalDateTime.now());
        planVersionRepo.save(pv);

        return nextPhase.getId();
    }

    private PhaseDto parseNextPhasePlan(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(sanitizeRawJson(rawJson));
            JsonNode planNode = extractPlanNode(root);

            if (planNode.isTextual()) {
                planNode = objectMapper.readTree(planNode.asText());
            }

            if (planNode.has("phase")) {
                return objectMapper.treeToValue(planNode.get("phase"), PhaseDto.class);
            }

            return objectMapper.treeToValue(planNode, PhaseDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid next phase JSON (strict parse failed).", e);
        }
    }

    private void validateNextPhase(PhaseDto dto) {
        Set<ConstraintViolation<PhaseDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Next phase validation failed: " + msg);
        }
    }

    private void enforceNextPhaseStructuralRules(PhaseDto dto){
        if(dto.getTasks() == null || dto.getTasks().isEmpty()){
            throw new IllegalArgumentException(
                    "Next phase '" + dto.getTitle() + "' cannot be empty");
        }
    }

    private String serializePhase(PhaseDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize normalized next phase JSON.", e);
        }
    }

    private void normalizeNextPhase(PhaseDto dto) {
        dto.setTitle(dto.getTitle().trim());
        dto.setTasks(dto.getTasks().stream()
                .peek(t -> {
                    t.setTitle(t.getTitle().trim());
                    t.setDescription(t.getDescription().trim());
                })
                .sorted(Comparator.comparingInt(t -> t.getDay()))
                .toList());
    }

    private void enforceNextPhaseLimits(PhaseDto dto, UUID goalId) {
        int existingTasks = taskRepo.countTotalTasks(goalId);
        int newTasks = dto.getTasks().size();
        if (existingTasks + newTasks > MAX_TOTAL_TASKS) {
            throw new IllegalArgumentException("Adding the next phase would exceed safe task limits: " + (existingTasks + newTasks) + " tasks (max " + MAX_TOTAL_TASKS + ")");
        }
    }

    private InitialGoalPlanDto parseInitialPlan(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(sanitizeRawJson(rawJson));
            JsonNode planNode = extractPlanNode(root);

            if (planNode.isTextual()) {
                return objectMapper.readValue(planNode.asText(), InitialGoalPlanDto.class);
            }

            return objectMapper.treeToValue(planNode, InitialGoalPlanDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid initial plan JSON (strict parse failed).", e);
        }
    }

    private void validateInitial(InitialGoalPlanDto dto) {
        Set<ConstraintViolation<InitialGoalPlanDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Initial plan validation failed: " + msg);
        }
    }

    private void enforceInitialStructuralRules(InitialGoalPlanDto dto){
        if(dto.getPhase() == null){
            throw new IllegalArgumentException("Initial plan must include a phase.");
        }

        if(dto.getPhase().getTasks() == null || dto.getPhase().getTasks().isEmpty()){
            throw new IllegalArgumentException(
                    "Initial phase '" + dto.getPhase().getTitle() + "' cannot be empty");
        }
    }

    private String serializeInitialPlan(InitialGoalPlanDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize normalized initial plan JSON.", e);
        }
    }

    private void normalizeInitial(InitialGoalPlanDto dto) {
        dto.setGoalTitle(dto.getGoalTitle().trim());
        dto.setGoalSummary(dto.getGoalSummary().trim());

        InitialPhaseDto phase = dto.getPhase();
        phase.setTitle(phase.getTitle().trim());
        phase.setTasks(phase.getTasks().stream()
                .peek(t -> {
                    t.setTitle(t.getTitle().trim());
                    t.setDescription(t.getDescription().trim());
                })
                .sorted(Comparator.comparingInt(t -> t.getDay()))
                .toList());
    }

    private void enforceInitialLimits(InitialGoalPlanDto dto) {
        int totalTasks = dto.getPhase().getTasks().size();
        if (totalTasks > MAX_TOTAL_TASKS) {
            throw new IllegalArgumentException("Initial plan too large: " + totalTasks + " tasks (max " + MAX_TOTAL_TASKS + ")");
        }
    }

    private void enforceStructuralRules(PlanDto dto){

        dto.getPhases().forEach(phase -> {

            if(phase.getTasks() == null || phase.getTasks().isEmpty()){
                throw new IllegalArgumentException(
                        "Phase '" + phase.getTitle() + "' cannot be empty");
            }
        });
    }

    private void enforceDuration(PlanDto dto){

        int total = dto.getPhases()
                .stream()
                .mapToInt(PhaseDto::getDurationDays)
                .sum();

        if(total > 3650){
            throw new IllegalArgumentException(
                    "Plan duration exceeds safe limit");
        }
    }


    private PlanDto parseStrict(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(sanitizeRawJson(rawJson));
            JsonNode planNode = extractPlanNode(root);

            if (planNode.isTextual()) {
                return objectMapper.readValue(planNode.asText(), PlanDto.class);
            }

            return objectMapper.treeToValue(planNode, PlanDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid plan JSON (strict parse failed).", e);
        }
    }

    private String sanitizeRawJson(String rawJson) {
        String trimmed = rawJson == null ? "" : rawJson.trim();
        if (trimmed.startsWith("{{")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }

    private JsonNode extractPlanNode(JsonNode root) {
        // Handle Chat Completions API response format
        if (root.has("choices")) {
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                JsonNode content = message.path("content");
                if (content.isTextual()) {
                    try {
                        return objectMapper.readTree(content.asText());
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Failed to parse content as JSON", e);
                    }
                }
            }
        }

        // Fallback to old Responses API format
        if (!root.has("output")) {
            return root;
        }

        for (JsonNode outputNode : root.path("output")) {
            for (JsonNode contentNode : outputNode.path("content")) {
                JsonNode textNode = contentNode.path("text");
                if (textNode.isTextual()) {
                    return textNode;
                }
            }
        }

        throw new IllegalArgumentException("LLM response is missing expected structure");
    }

    private String serializePlan(PlanDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize normalized plan JSON.", e);
        }
    }

    private void validate(PlanDto dto) {
        Set<ConstraintViolation<PlanDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Plan validation failed: " + msg);
        }
    }

    private PlanDto normalize(PlanDto dto) {
        // v1 normalization: trim text, remove empty tasks (already blocked by validation), stable order by day.
        dto.setGoalTitle(dto.getGoalTitle().trim());

        dto.getPhases().forEach(p -> {
            p.setTitle(p.getTitle().trim());
            p.getTasks().forEach(t -> {
                t.setTitle(t.getTitle().trim());
                t.setDescription(t.getDescription().trim());
            });
            p.setTasks(p.getTasks().stream()
                    .sorted(Comparator.comparingInt(t -> t.getDay()))
                    .toList());
        });

        return dto;
    }

    private void enforceLimits(PlanDto dto) {
        int totalTasks = dto.getPhases().stream().mapToInt(p -> p.getTasks().size()).sum();
        if (totalTasks > MAX_TOTAL_TASKS) {
            throw new IllegalArgumentException("Plan too large: " + totalTasks + " tasks (max " + MAX_TOTAL_TASKS + ")");
        }
    }

    private void saveTaskQuestions(UUID taskId, List<TaskQuestionInputDto> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }

        for (int i = 0; i < questions.size(); i++) {
            TaskQuestionInputDto questionDto = questions.get(i);
            TaskQuestion question = new TaskQuestion();
            question.setId(UUID.randomUUID());
            question.setTaskId(taskId);
            question.setQuestionIndex(i);
            question.setQuestion(questionDto.getQuestion());
            question.setQuestionType(questionDto.getType());
            question.setHint(questionDto.getHint());

            taskQuestionRepo.save(question);
        }
    }
}

