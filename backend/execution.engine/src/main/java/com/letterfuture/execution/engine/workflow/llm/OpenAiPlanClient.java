package com.letterfuture.execution.engine.workflow.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OpenAiPlanClient {

    private static final String GOAL_PLAN_PAYLOAD_TEMPLATE = """
            {
  "model": "gpt-4o-mini",
  "input": [
    {
      "role": "developer",
      "content": "You are a planning engine for an execution app. Return only valid JSON matching the schema and satisfy every validation rule exactly. Create a complete, practical execution plan based on this goal description: %s. Hard requirements: the plan must contain exactly 5 phases; target_duration_days must be an integer between 30 and 3650; each phase must have duration_days between 7 and 365; each phase must include 1 to 8 tasks; every task title and description must be non-empty; every task day must be an integer between 1 and 3650; keep the response detailed and actionable; do not include any prose outside the JSON."
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
          "goal_title": {
            "type": "string",
            "minLength": 1
          },
          "target_duration_days": {
            "type": "integer",
            "minimum": 30,
            "maximum": 3650
          },
          "phases": {
            "type": "array",
            "minItems": 5,
            "maxItems": 5,
            "items": {
              "type": "object",
              "additionalProperties": false,
              "properties": {
                "title": {
                  "type": "string",
                  "minLength": 1
                },
                "duration_days": {
                  "type": "integer",
                  "minimum": 7,
                  "maximum": 365
                },
                "tasks": {
                  "type": "array",
                  "minItems": 1,
                  "maxItems": 8,
                  "items": {
                    "type": "object",
                    "additionalProperties": false,
                    "properties": {
                      "title": {
                        "type": "string",
                        "minLength": 1
                      },
                      "description": {
                        "type": "string",
                        "minLength": 1,
                        "maxLength": 2000
                      },
                      "day": {
                        "type": "integer",
                        "minimum": 1,
                        "maximum": 3650
                      }
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
      "content": "You are a planning engine for an execution app. Return only valid JSON matching the schema and satisfy every validation rule exactly. Generate an initial plan for this goal description: %s. The response must include a short goal interpretation, a total estimated journey outline, and only the first phase with tasks. For each task, provide 3-4 simple data collection questions that help track progress (e.g., current weight, completed reps, hours invested, completion status, etc). Do not generate future phases yet. Hard requirements: goal_summary must be non-empty; target_duration_days must be an integer between 30 and 3650; the phase must have a title; duration_days must be an integer between 7 and 365; tasks must contain 1 to 8 items; every task must have 3-4 questions; every question must be concise and answerable; every task title and description must be non-empty; every task day must be an integer between 1 and 3650; do not include any prose outside the JSON."
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
          "goal_title": {
            "type": "string",
            "minLength": 1
          },
          "goal_summary": {
            "type": "string",
            "minLength": 1
          },
          "target_duration_days": {
            "type": "integer",
            "minimum": 30,
            "maximum": 3650
          },
          "phase": {
            "type": "object",
            "additionalProperties": false,
            "properties": {
              "title": {
                "type": "string",
                "minLength": 1
              },
              "duration_days": {
                "type": "integer",
                "minimum": 7,
                "maximum": 365
              },
              "tasks": {
                "type": "array",
                "minItems": 1,
                "maxItems": 8,
                "items": {
                  "type": "object",
                  "additionalProperties": false,
                  "properties": {
                    "title": {
                      "type": "string",
                      "minLength": 1
                    },
                    "description": {
                      "type": "string",
                      "minLength": 1,
                      "maxLength": 2000
                    },
                    "day": {
                      "type": "integer",
                      "minimum": 1,
                      "maximum": 3650
                    },
                    "questions": {
                      "type": "array",
                      "minItems": 3,
                      "maxItems": 4,
                      "items": {
                        "type": "object",
                        "additionalProperties": false,
                        "properties": {
                          "question": {
                            "type": "string",
                            "minLength": 5
                          },
                          "type": {
                            "type": "string",
                            "enum": ["text", "number", "date", "boolean"]
                          },
                          "hint": {
                            "type": "string"
                          }
                        },
                        "required": ["question", "type", "hint"]
                      }
                    }
                  },
                  "required": ["title", "description", "day"]
                }
              }
            },
            "required": ["title", "duration_days", "tasks"]
          }
        },
        "required": ["goal_title", "goal_summary", "target_duration_days", "phase"]
      }
    }
  }
}
            """;

    private static final String NEXT_PHASE_PAYLOAD_TEMPLATE = """
            {
  "model": "gpt-4o-mini",
  "messages": [
    {
      "role": "system",
      "content": "You are a planning engine for an execution app. Return only valid JSON matching the schema and rules exactly. For the goal '%s' with this phase history:\\n%s\\nand current progress with user responses:\\n%s\\nDecide if another phase is needed. Consider the user's actual progress and responses when making this decision. If the goal appears complete based on the history and progress, return {\\"complete\\": true}. If another phase is needed, return {\\"complete\\": false, \\"phase\\": {...}} with the next phase details. For each task, provide 3-4 simple data collection questions that help track progress and are customized based on the user's previous responses. Hard requirements: if complete=true, do not include phase field; if complete=false, phase.title must be non-empty; duration_days must be an integer between 7 and 365; tasks must contain 1 to 8 items; every task must have 3-4 questions; every task title and description must be non-empty; every task day must be an integer between 1 and 3650; do not include any prose outside the JSON."
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
          "complete": {
            "type": "boolean"
          },
          "phase": {
            "type": "object",
            "additionalProperties": false,
            "properties": {
              "title": {
                "type": "string",
                "minLength": 1
              },
              "duration_days": {
                "type": "integer",
                "minimum": 7,
                "maximum": 365
              },
              "tasks": {
                "type": "array",
                "minItems": 1,
                "maxItems": 8,
                "items": {
                  "type": "object",
                  "additionalProperties": false,
                  "properties": {
                    "title": {
                      "type": "string",
                      "minLength": 1
                    },
                    "description": {
                      "type": "string",
                      "minLength": 1,
                      "maxLength": 2000
                    },
                    "day": {
                      "type": "integer",
                      "minimum": 1,
                      "maximum": 3650
                    },
                    "questions": {
                      "type": "array",
                      "minItems": 3,
                      "maxItems": 4,
                      "items": {
                        "type": "object",
                        "additionalProperties": false,
                        "properties": {
                          "question": {
                            "type": "string",
                            "minLength": 5
                          },
                          "type": {
                            "type": "string",
                            "enum": ["text", "number", "date", "boolean"]
                          },
                          "hint": {
                            "type": "string"
                          }
                        },
                        "required": ["question", "type", "hint"]
                      }
                    }
                  },
                  "required": ["title", "description", "day"]
                }
              }
            },
            "required": ["title", "duration_days", "tasks"]
          }
        },
        "required": ["complete"],
        "if": {
          "properties": { "complete": { "const": false } }
        },
        "then": {
          "required": ["phase"]
        }
      }
    }
  }
}
            """;

    private final WebClient webClient;
    private final String apiKey;

    public OpenAiPlanClient(
            WebClient.Builder webClientBuilder,
            @Value("${openai.base-url}") String baseUrl,
            @Value("${openai.api-key:}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public String generateGoalPlan(String goalDescription) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured.");
        }

        String payload = buildGoalPlanPayload(goalDescription);

        String response = webClient.post()
                .uri("/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (response == null || response.isBlank()) {
            throw new IllegalStateException("OpenAI returned an empty response.");
        }

        return response;
    }

    public String generateInitialGoalPlan(String goalDescription) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured.");
        }

        String payload = buildInitialGoalPlanPayload(goalDescription);

        String response = webClient.post()
                .uri("/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (response == null || response.isBlank()) {
            throw new IllegalStateException("OpenAI returned an empty response.");
        }

        return response;
    }

    public String generateNextPhasePlan(String goalTitle,
                                       String priorPhasesSummary,
                                       String progressSummary) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured.");
        }

        String payload = buildNextPhasePayload(goalTitle, priorPhasesSummary, progressSummary);

        String response = webClient.post()
                .uri("/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (response == null || response.isBlank()) {
            throw new IllegalStateException("OpenAI returned an empty response.");
        }

        return response;
    }

    private String buildGoalPlanPayload(String goalDescription) {
        return GOAL_PLAN_PAYLOAD_TEMPLATE.formatted(escapeJson(goalDescription == null ? "" : goalDescription.trim()));
    }

    private String buildInitialGoalPlanPayload(String goalDescription) {
        return INITIAL_GOAL_PLAN_PAYLOAD_TEMPLATE.formatted(escapeJson(goalDescription == null ? "" : goalDescription.trim()));
    }

    private String buildNextPhasePayload(String goalTitle,
                                         String priorPhasesSummary,
                                         String progressSummary) {
        return NEXT_PHASE_PAYLOAD_TEMPLATE.formatted(
                escapeJson(goalTitle == null ? "" : goalTitle.trim()),
                escapeJson(priorPhasesSummary == null ? "" : priorPhasesSummary.trim()),
                escapeJson(progressSummary == null ? "" : progressSummary.trim()));
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
