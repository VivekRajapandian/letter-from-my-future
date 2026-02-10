package com.letterfuture.execution.engine.workflow.compiler.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class PlanDto {

    @NotBlank
    private String goalTitle;

    @Min(30) @Max(3650)
    private int targetDurationDays;

    @NotEmpty
    @Size(max = 5)
    @Valid
    private List<PhaseDto> phases;
}
