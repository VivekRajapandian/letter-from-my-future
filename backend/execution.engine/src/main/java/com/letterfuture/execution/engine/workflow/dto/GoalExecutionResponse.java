package com.letterfuture.execution.engine.workflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoalExecutionResponse(
        GoalView goal,
        PlanningView planning,
        ActivePhaseView activePhase,
        List<TaskExecutionView> tasks,
        ProgressView progress
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GoalView(
            UUID goalId,
            String title,
            String summary,
            String status,
            String planningMode,
            Integer targetDurationDays,
            Integer phaseCountPlanned,
            Integer phaseCountCreated
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PlanningView(
            String state,
            boolean canGenerateNextPhase,
            String reason
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ActivePhaseView(
            UUID phaseId,
            String title,
            String status,
            Integer orderIndex,
            Integer durationDays,
            String outlineTitle
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TaskExecutionView(
            UUID taskId,
            String title,
            String status,
            Integer orderIndex,
            Integer scheduledDay,
            InstructionView instruction,
            List<InputFieldView> inputSchema,
            SubmissionView latestSubmission
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InstructionView(
            String what,
            String how,
            String why,
            String successCriteria
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InputFieldView(
            UUID fieldId,
            String key,
            String label,
            String type,
            boolean required,
            String placeholder
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubmissionView(
            LocalDateTime submittedAt,
            Map<String, Object> values
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ProgressView(
            long completedTasks,
            long totalVisibleTasks,
            int phaseProgressPercent,
            int goalProgressPercent
    ) {}
}
