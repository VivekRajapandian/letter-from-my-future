package com.letterfuture.execution.engine.workflow.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class InitialGoalPlanDto {

    @NotBlank
    @JsonAlias({"goalTitle", "goal_title"})
    private String goalTitle;

    @NotBlank
    @JsonAlias({"goalSummary", "goal_summary"})
    private String goalSummary;

    @Min(30)
    @Max(3650)
    @JsonAlias({"targetDurationDays", "target_duration_days"})
    private int targetDurationDays;

    @Min(3)
    @Max(5)
    @JsonAlias({"totalPhases", "total_phases"})
    private int totalPhases;

    @JsonAlias({"phaseOutline", "phase_outline"})
    private List<String> phaseOutline;

    @NotNull
    @Valid
    private InitialPhaseDto phase;
}
