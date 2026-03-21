package com.letterfuture.execution.engine.workflow.dto;


import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class PhaseDto {

    @NotBlank
    private String title;

    @Min(7) @Max(365)
    @JsonAlias({"durationDays", "duration_days"})
    private int durationDays;

    @NotEmpty
    @Size(max = 8)
    @Valid
    private List<TaskDto> tasks;
}

