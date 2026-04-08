package com.letterfuture.execution.engine.workflow.dto;


import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class TaskDto {

    @NotBlank
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String description;

    @Min(1) @Max(3650)
    private int day;

    @Valid
    @Size(min = 0, max = 4)
    private List<TaskQuestionInputDto> questions;
}
