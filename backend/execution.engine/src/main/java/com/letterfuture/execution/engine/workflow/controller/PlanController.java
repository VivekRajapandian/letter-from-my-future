package com.letterfuture.execution.engine.workflow.controller;


import com.letterfuture.execution.engine.workflow.compiler.PlanCompiler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/plans")
public class PlanController {

    private final PlanCompiler compiler;

    @PostMapping("/compile")
    public UUID compile(@RequestParam UUID userId, @RequestBody String rawJson){
        return compiler.compileAndCreateGoal(userId, rawJson);
    }
}
