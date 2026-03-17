package com.letterfuture.execution.engine.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class MeResponse {

    private UUID userId;
    private String username;
    private String email;
    private String role;
}