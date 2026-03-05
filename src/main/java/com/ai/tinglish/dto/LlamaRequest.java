package com.ai.tinglish.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "llama")
@Validated
public record LlamaRequest(
    @NotBlank(message = "llama.prompt must not be blank or null")
    String prompt,
    Model model,
    Api api,
    boolean stream) {

    public LlamaRequest {
        // Provide sensible defaults for nested objects if missing
        if (model == null) model = new Model("llama3");
        if (api == null) api = new Api("http://localhost:11434", "/api/generate");
    }

    public record Model(String name) {}
    public record Api(String url, String endpoint) {}
}
