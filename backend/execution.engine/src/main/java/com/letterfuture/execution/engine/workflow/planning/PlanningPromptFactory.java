package com.letterfuture.execution.engine.workflow.planning;

import org.springframework.stereotype.Component;

@Component
public class PlanningPromptFactory {

    private static final String GOAL_PLAN_PAYLOAD_TEMPLATE = """
            {
              "model": "gpt-4o-mini",
              "input": [
                {
                  "role": "developer",
                  "content": "You are a planning engine for an execution app.\\nReturn only valid JSON matching the schema and satisfy every validation rule exactly. Create a complete, practical execution plan based on this goal description: %s.\\nHard requirements: the plan must contain exactly 5 phases; target_duration_days must be an integer between 30 and 3650; each phase must have duration_days between 7 and 365; each phase must include 1 to 8 tasks; every task title and description must be non-empty; every task day must be an integer between 1 and 3650; keep the response detailed and actionable; do not include any prose outside the JSON."
                }
              ],
              "text": {
                "format": {
                  "type": "json_schema",
                  "name": "goal_plan",
                  "schema": {
                    "type": "object",
                    "additionalProperties": false,
                    "properties": {
                      "goal_title": { "type": "string", "minLength": 1 },
                      "target_duration_days": { "type": "integer", "minimum": 30, "maximum": 3650 },
                      "phases": {
                        "type": "array",
                        "minItems": 5,
                        "maxItems": 5,
                        "items": {
                          "type": "object",
                          "additionalProperties": false,
                          "properties": {
                            "title": { "type": "string", "minLength": 1 },
                            "duration_days": { "type": "integer", "minimum": 7, "maximum": 365 },
                            "tasks": {
                              "type": "array",
                              "minItems": 1,
                              "maxItems": 8,
                              "items": {
                                "type": "object",
                                "additionalProperties": false,
                                "properties": {
                                  "title": { "type": "string", "minLength": 1 },
                                  "description": { "type": "string", "minLength": 1, "maxLength": 2000 },
                                  "day": { "type": "integer", "minimum": 1, "maximum": 3650 }
                                },
                                "required": ["title", "description", "day"]
                              }
                            }
                          },
                          "required": ["title", "duration_days", "tasks"]
                        }
                      }
                    },
                    "required": ["goal_title", "target_duration_days", "phases"]
                  }
                }
              }
            }
            """;

    private static final String INITIAL_GOAL_PLAN_PAYLOAD_TEMPLATE = """
            {
              "model": "gpt-4o-mini",
              "input": [
                {
                  "role": "developer",
                  "content": "You are a planning engine for an execution app.\\nReturn only valid JSON matching the schema and satisfy every validation rule exactly. Generate a complete initial plan for this goal description: %s. First, plan out ALL phases of the journey (3-5 phases). Then provide: 1) A short goal interpretation, 2) Total estimated timeline, 3) An outline of all phase titles showing the complete journey, 4) Only the detailed first phase with tasks and questions. For each task, provide 3-4 simple data collection questions that help track progress.\\nHard requirements: goal_summary must be non-empty; target_duration_days must be an integer between 30 and 3650; total_phases must be between 3 and 5; phase_outline must be an array with exact same length as total_phases with meaningful phase titles; the initial phase must have a title; duration_days must be an integer between 7 and 365; tasks must contain 1 to 8 items; every task must have 3-4 questions; every question must be concise and answerable; every task title and description must be non-empty; every task day must be an integer between 1 and 3650; do not include any prose outside the JSON."
                }
              ],
              "text": {
                "format": {
                  "type": "json_schema",
                  "name": "initial_goal_plan",
                  "schema": {
                    "type": "object",
                    "additionalProperties": false,
                    "properties": {
                      "goal_title": { "type": "string", "minLength": 1 },
                      "goal_summary": { "type": "string", "minLength": 1 },
                      "target_duration_days": { "type": "integer", "minimum": 30, "maximum": 3650 },
                      "total_phases": { "type": "integer", "minimum": 3, "maximum": 5 },
                      "phase_outline": {
                        "type": "array",
                        "minItems": 3,
                        "maxItems": 5,
                        "items": { "type": "string", "minLength": 1 }
                      },
                      "phase": {
                        "type": "object",
                        "additionalProperties": false,
                        "properties": {
                          "title": { "type": "string", "minLength": 1 },
                          "duration_days": { "type": "integer", "minimum": 7, "maximum": 365 },
                          "tasks": {
                            "type": "array",
                            "minItems": 1,
                            "maxItems": 8,
                            "items": {
                              "type": "object",
                              "additionalProperties": false,
                              "properties": {
                                "title": { "type": "string", "minLength": 1 },
                                "description": { "type": "string", "minLength": 1, "maxLength": 2000 },
                                "day": { "type": "integer", "minimum": 1, "maximum": 3650 },
                                "questions": {
                                  "type": "array",
                                  "minItems": 3,
                                  "maxItems": 4,
                                  "items": {
                                    "type": "object",
                                    "additionalProperties": false,
                                    "properties": {
                                      "question": { "type": "string", "minLength": 5 },
                                      "type": {
                                        "type": "string",
                                        "enum": ["text", "number", "date", "boolean"]
                                      },
                                      "hint": { "type": "string" }
                                    },
                                    "required": ["question", "type", "hint"]
                                  }
                                }
                              },
                              "required": ["title", "description", "day", "questions"]
                            }
                          }
                        },
                        "required": ["title", "duration_days", "tasks"]
                      }
                    },
                    "required": [
                      "goal_title",
                      "goal_summary",
                      "target_duration_days",
                      "total_phases",
                      "phase_outline",
                      "phase"
                    ]
                  }
                }
              }
            }
            """;

    private static final String NEXT_PHASE_PAYLOAD_TEMPLATE = """
            {
              "model": "gpt-4o-mini",
              "input": [
                {
                  "role": "developer",
                  "content": "You are a planning engine for an execution app.\\nReturn only valid JSON matching the schema and rules exactly. For the goal '%s', the user is completing Phase %d of %d total phases. Here is the phase history:\\n%s\\nThe user just completed the previous phase with these responses:\\n%s\\nDecide the next phase based on the user's actual progress and responses. Consider if the user is tracking well or needs adjustments. If this is the final phase (phase number equals total phases) and progress looks good, then return {\\\"complete\\\": true, \\\"phase\\\": null}.\\nOtherwise, generate the next phase with customized tasks and questions that build on the user's previous progress.\\nHard requirements: if phase_number equals total_phases, determine completion based on user progress; if complete=true, set phase to null; if complete=false, phase must be present with a non-empty title; duration_days must be an integer between 7 and 365; tasks must contain 1 to 8 items; every task must have 3-4 questions customized for the user's responses; every task title and description must be non-empty; every task day must be an integer between 1 and 3650; do not include any prose outside the JSON."
                }
              ],
              "text": {
                "format": {
                  "type": "json_schema",
                  "name": "next_phase_decision",
                  "schema": {
                    "type": "object",
                    "additionalProperties": false,
                    "properties": {
                      "complete": { "type": "boolean" },
                      "phase": {
                        "type": ["object", "null"],
                        "additionalProperties": false,
                        "properties": {
                          "title": { "type": "string", "minLength": 1 },
                          "duration_days": { "type": "integer", "minimum": 7, "maximum": 365 },
                          "tasks": {
                            "type": "array",
                            "minItems": 1,
                            "maxItems": 8,
                            "items": {
                              "type": "object",
                              "additionalProperties": false,
                              "properties": {
                                "title": { "type": "string", "minLength": 1 },
                                "description": { "type": "string", "minLength": 1, "maxLength": 2000 },
                                "day": { "type": "integer", "minimum": 1, "maximum": 3650 },
                                "questions": {
                                  "type": "array",
                                  "minItems": 3,
                                  "maxItems": 4,
                                  "items": {
                                    "type": "object",
                                    "additionalProperties": false,
                                    "properties": {
                                      "question": { "type": "string", "minLength": 5 },
                                      "type": {
                                        "type": "string",
                                        "enum": ["text", "number", "date", "boolean"]
                                      },
                                      "hint": { "type": "string" }
                                    },
                                    "required": ["question", "type", "hint"]
                                  }
                                }
                              },
                              "required": ["title", "description", "day", "questions"]
                            }
                          }
                        },
                        "required": ["title", "duration_days", "tasks"]
                      }
                    },
                    "required": ["complete", "phase"]
                  }
                }
              }
            }
            """;

    public String buildGoalPlanPayload(String goalDescription) {
        return GOAL_PLAN_PAYLOAD_TEMPLATE.formatted(
                escapeJson(normalize(goalDescription))
        );
    }

    public String buildInitialGoalPlanPayload(String goalDescription) {
        return INITIAL_GOAL_PLAN_PAYLOAD_TEMPLATE.formatted(
                escapeJson(normalize(goalDescription))
        );
    }

    public String buildNextPhasePayload(NextPhasePromptRequest request) {
        return NEXT_PHASE_PAYLOAD_TEMPLATE.formatted(
                escapeJson(normalize(request.goalTitle())),
                request.currentPhaseNumber(),
                request.totalPhases(),
                escapeJson(normalize(request.priorPhasesSummary())),
                escapeJson(normalize(request.progressSummary()))
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    public record NextPhasePromptRequest(
            String goalTitle,
            int currentPhaseNumber,
            int totalPhases,
            String priorPhasesSummary,
            String progressSummary
    ) {
    }

    public record GoalPlanPromptRequest(
            String goalDescription
    ) {
    }

    public record InitialGoalPlanPromptRequest(
            String goalDescription
    ) {
    }

    public String buildGoalPlanPayload(GoalPlanPromptRequest request) {
        return buildGoalPlanPayload(request.goalDescription());
    }

    public String buildInitialGoalPlanPayload(InitialGoalPlanPromptRequest request) {
        return buildInitialGoalPlanPayload(request.goalDescription());
    }
}