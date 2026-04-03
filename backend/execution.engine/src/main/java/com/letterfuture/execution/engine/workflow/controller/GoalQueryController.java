package com.letterfuture.execution.engine.workflow.controller;

import com.letterfuture.execution.engine.workflow.dto.GoalSummaryResponse;
import com.letterfuture.execution.engine.workflow.dto.OngoingGoalsResponse;
import com.letterfuture.execution.engine.workflow.service.GoalQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class GoalQueryController {

    private final GoalQueryService goalQueryService;

    @GetMapping("/users/{userId}/goals/ongoing")
    public OngoingGoalsResponse getOngoingGoals(@PathVariable UUID userId) {
        return goalQueryService.getOngoingGoals(userId);
    }

    @GetMapping("/goals/{goalId}/summary")
    public GoalSummaryResponse getGoalSummary(@PathVariable UUID goalId,
                                              @RequestParam UUID userId) {
        return goalQueryService.getGoalSummary(goalId, userId);
    }
}
