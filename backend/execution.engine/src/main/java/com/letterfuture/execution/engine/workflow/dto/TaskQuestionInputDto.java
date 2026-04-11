package com.letterfuture.execution.engine.workflow.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskQuestionInputDto {

    @NotBlank
    private String question;

    @NotBlank
    @JsonAlias({"type", "questionType"})
    private String type; // text, number, date, boolean

    private String hint;
}
