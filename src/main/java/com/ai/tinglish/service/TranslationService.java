package com.ai.tinglish.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import java.util.Map;
import com.ai.tinglish.dto.LlamaRequest;

@Service
public class TranslationService {

    private final WebClient webClient;
    private final LlamaRequest llamaRequest;

    public TranslationService(WebClient webClient, LlamaRequest llamaRequest) {
        this.webClient = webClient;
        this.llamaRequest = llamaRequest;
    }

    /**
     * Use default properties from LlamaRequest record
     */
    public String callModel(String prompt) {
        String model = llamaRequest.model().name();
        boolean stream = llamaRequest.stream();
        String defaultPrompt = llamaRequest.prompt();
        String effectivePrompt = (prompt == null || prompt.isBlank()) ? defaultPrompt : prompt;
        String baseUrl = llamaRequest.api().url();
        String endpoint = llamaRequest.api().endpoint();
        String fullUrl = baseUrl + endpoint;
        return callModel(effectivePrompt, model, fullUrl, stream);
    }

    /**
     * Dynamic call where caller supplies model, fullUrl and stream flag at runtime
     */
    public String callModel(String prompt, String model, String fullUrl, boolean stream) {
        Map<String, Object> request = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", stream
        );

        return webClient.post()
                .uri(fullUrl)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(n -> {
                    JsonNode resp = n.path("response");
                    if (resp.isNull() || resp.isMissingNode()) {
                        return "";
                    }
                    return resp.asText("");
                })
                 .block();
    }
}
