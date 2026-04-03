package com.letterfuture.execution.engine.workflow.service;


import com.letterfuture.execution.engine.enums.GoalStatus;
import com.letterfuture.execution.engine.workflow.domain.Goal;
import com.letterfuture.execution.engine.workflow.dto.GoalCardResponse;
import com.letterfuture.execution.engine.workflow.dto.GoalSummaryResponse;
import com.letterfuture.execution.engine.workflow.dto.NextTaskResponse;
import com.letterfuture.execution.engine.workflow.dto.OngoingGoalsResponse;
import com.letterfuture.execution.engine.workflow.engine.WorkflowEngine;
import com.letterfuture.execution.engine.workflow.repository.GoalRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalQueryService {

    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;
    private final WorkflowEngine workflowEngine;

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
}