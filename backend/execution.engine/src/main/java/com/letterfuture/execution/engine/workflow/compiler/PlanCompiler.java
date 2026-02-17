package com.letterfuture.execution.engine.workflow.compiler;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.letterfuture.execution.engine.enums.GoalStatus;
import com.letterfuture.execution.engine.enums.TaskStatus;
import com.letterfuture.execution.engine.workflow.dto.PlanDto;
import com.letterfuture.execution.engine.workflow.dto.PhaseDto;
import com.letterfuture.execution.engine.workflow.domain.*;
import com.letterfuture.execution.engine.workflow.repository.*;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        pv.setPlanJson(rawPlanJson);
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
            return objectMapper.readValue(rawJson, PlanDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid plan JSON (strict parse failed).", e);
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
}

