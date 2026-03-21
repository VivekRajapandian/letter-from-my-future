package com.letterfuture.execution.engine.workflow.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class PlanDto {

    @NotBlank
    @JsonAlias({"goalTitle", "goal_title"})
    private String goalTitle;

    @Min(30) @Max(3650)
    @JsonAlias({"targetDurationDays", "target_duration_days"})
    private int targetDurationDays;

    @NotEmpty
    @Size(max = 5)
    @Valid
    private List<PhaseDto> phases;
}
