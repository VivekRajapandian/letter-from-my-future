package com.letterfuture.execution.engine.workflow.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank
    private String usernameOrEmail;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;
}