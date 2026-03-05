package com.ai.tinglish.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TranslationRequest(

        @NotBlank(message = "Text must not be empty")
        @Size(max = 5000, message = "Text exceeds maximum allowed length")
        String text,

        @NotBlank(message = "Source language is required")
        String sourceLanguage,

        @NotBlank(message = "Target language is required")
        String targetLanguage

) {}
