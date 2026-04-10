package com.letterfuture.execution.engine.workflow.execution;

import com.letterfuture.execution.engine.workflow.domain.Goal;
import com.letterfuture.execution.engine.workflow.domain.Phase;
import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.workflow.dto.execution.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExecutionSnapshotAssembler {

    public ExecutionSnapshotResponse toResponse(
            Goal goal,
            List<Phase> phases,
            Phase activePhase,
            List<Task> tasks
    ) {
        return new ExecutionSnapshotResponse(
                toGoalDto(goal, phases),
                toPlanningDto(goal, activePhase, tasks),
                toActivePhaseDto(activePhase),
                toTaskDtos(tasks),
                toProgressDto(phases, tasks)
        );
    }

    private ExecutionGoalDto toGoalDto(Goal goal, List<Phase> phases) {
        int phaseCountCreated = phases == null ? 0 : phases.size();

        Integer phaseCountPlanned = goal.getTotalPhasesPlanned() != null
                ? goal.getTotalPhasesPlanned()
                : phaseCountCreated;

        return new ExecutionGoalDto(
                goal.getId(),
                goal.getTitle(),
                goal.getSummary(),
                goal.getStatus().toString(),
                goal.getPlanningMode(),
                goal.getTargetDurationDays(),
                phaseCountPlanned,
                phaseCountCreated
        );
    }

    private ExecutionPlanningDto toPlanningDto(Goal goal, Phase activePhase, List<Task> tasks) {
        String planningState = goal.getPlanningState() != null
                ? goal.getPlanningState()
                : derivePlanningState(goal, activePhase, tasks);

        boolean canGenerateNextPhase = false;
        String reason = derivePlanningReason(planningState, activePhase, tasks);

        return new ExecutionPlanningDto(
                planningState,
                canGenerateNextPhase,
                reason
        );
    }

    private String derivePlanningState(Goal goal, Phase activePhase, List<Task> tasks) {
        if (equalsIgnoreCase(goal.getStatus().name(), "COMPLETED")) {
            return "COMPLETED";
        }

        if (activePhase == null) {
            return "GENERATING_NEXT_PHASE";
        }

        if (tasks == null || tasks.isEmpty()) {
            return "WAITING_FOR_USER_INPUT";
        }

        return "READY";
    }

    private String derivePlanningReason(String planningState, Phase activePhase, List<Task> tasks) {
        if ("COMPLETED".equalsIgnoreCase(planningState)) {
            return null;
        }

        if ("GENERATING_NEXT_PHASE".equalsIgnoreCase(planningState)) {
            return "No active phase is currently available.";
        }

        if ("WAITING_FOR_USER_INPUT".equalsIgnoreCase(planningState)) {
            return (activePhase != null && (tasks == null || tasks.isEmpty()))
                    ? "Active phase exists but no visible tasks are available yet."
                    : "Waiting for user input.";
        }

        return null;
    }

    private ExecutionPhaseDto toActivePhaseDto(Phase activePhase) {
        if (activePhase == null) {
            return null;
        }

        return new ExecutionPhaseDto(
                activePhase.getId(),
                activePhase.getTitle(),
                activePhase.getStatus().toString(),
                activePhase.getOrderIndex(),
                activePhase.getDurationDays(),
                activePhase.getTitle()
        );
    }

    private List<ExecutionTaskDto> toTaskDtos(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }

        return tasks.stream()
                .map(this::toTaskDto)
                .toList();
    }

    private ExecutionTaskDto toTaskDto(Task task) {
        return new ExecutionTaskDto(
                task.getId(),
                task.getTitle(),
                task.getStatus().toString(),
                task.getOrderIndex(),
                task.getScheduledDay(),
                new TaskInstructionDto(
                        task.getInstructionWhat(),
                        task.getInstructionHow(),
                        task.getInstructionWhy(),
                        task.getSuccessCriteria()
                ),
                List.of(),
                null
        );
    }

    private ExecutionProgressDto toProgressDto(List<Phase> phases, List<Task> tasks) {
        int totalVisibleTasks = tasks == null ? 0 : tasks.size();
        int completedTasks = tasks == null ? 0 : (int) tasks.stream()
                .filter(this::isCompletedTask)
                .count();

        int phaseProgressPercent = totalVisibleTasks == 0
                ? 0
                : (completedTasks * 100) / totalVisibleTasks;

        int totalPhases = phases == null ? 0 : phases.size();
        int completedPhases = phases == null ? 0 : (int) phases.stream()
                .filter(this::isCompletedPhase)
                .count();

        int goalProgressPercent;
        if (totalPhases == 0) {
            goalProgressPercent = phaseProgressPercent;
        } else {
            goalProgressPercent = (completedPhases * 100) / totalPhases;
            if (goalProgressPercent < 100 && phaseProgressPercent > 0 && completedPhases < totalPhases) {
                int perPhaseWeight = 100 / totalPhases;
                goalProgressPercent += phaseProgressPercent / Math.max(perPhaseWeight, 1);
                if (goalProgressPercent > 99) {
                    goalProgressPercent = 99;
                }
            }
        }

        return new ExecutionProgressDto(
                completedTasks,
                totalVisibleTasks,
                phaseProgressPercent,
                goalProgressPercent
        );
    }

    private boolean isCompletedTask(Task task) {
        return equalsIgnoreCase(task.getStatus().toString(), "COMPLETED")
                || equalsIgnoreCase(task.getStatus().toString(), "DONE");
    }

    private boolean isCompletedPhase(Phase phase) {
        return equalsIgnoreCase(phase.getStatus().toString(), "COMPLETED")
                || equalsIgnoreCase(phase.getStatus().toString(), "DONE");
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && left.equalsIgnoreCase(right);
    }
}