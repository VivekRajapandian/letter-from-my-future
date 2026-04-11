package com.letterfuture.execution.engine.workflow.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OpenAiPlanClient {

    private final WebClient webClient;
    private final String apiKey;

    public OpenAiPlanClient(
            WebClient.Builder webClientBuilder,
            @Value("${openai.base-url}") String baseUrl,
            @Value("${openai.api-key:}") String apiKey
    ) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public String executeResponsesPayload(String payload) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured.");
        }

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
}