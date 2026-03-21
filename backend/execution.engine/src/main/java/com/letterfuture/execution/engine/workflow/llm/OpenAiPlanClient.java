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
      },
      "strict": true
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

    private String buildGoalPlanPayload(String goalDescription) {
        return GOAL_PLAN_PAYLOAD_TEMPLATE.formatted(escapeJson(goalDescription == null ? "" : goalDescription.trim()));
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
