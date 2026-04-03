package com.letterfuture.execution.engine.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OngoingGoalsResponse {
    UUID userId;
    boolean hasOngoingGoals;
    List<GoalCardResponse> goals;
}
