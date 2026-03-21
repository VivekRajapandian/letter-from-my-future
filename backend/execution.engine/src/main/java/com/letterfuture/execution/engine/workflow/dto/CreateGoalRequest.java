package com.letterfuture.execution.engine.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGoalRequest {

    @NotBlank
    private String goalDescription;
}
