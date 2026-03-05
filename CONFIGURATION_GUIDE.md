# LlamaRequest Configuration Guide

## Overview
The `LlamaRequest` is a Spring Boot configuration properties record that binds to the `llama.*` prefix in YAML/property files. It enforces mandatory validation using Jakarta validators (from `spring-boot-starter-validation`).

## Configuration Properties

### Required Properties
- **`llama.prompt`** ⚠️ **MANDATORY**
  - Description: Default prompt template to send to the Llama model
  - Type: String
  - Validation: Cannot be null or blank (enforced via `@NotBlank`)
  - Example: `"Translate the following text to the target language: {{text}}"`
  - Error if missing: `ConstraintViolationException: llama.prompt must not be blank or null`

### Optional Properties (with defaults)
- **`llama.api.url`**
  - Description: Base URL of the Llama/Ollama API server
  - Type: String
  - Default: `http://localhost:11434`
  - Example: `http://127.0.0.1:11434` or `http://host.docker.internal:11434`

- **`llama.api.endpoint`**
  - Description: API endpoint path for generation
  - Type: String
  - Default: `/api/generate`
  - Example: `/api/generate` or `/v1/completions`

- **`llama.model.name`**
  - Description: Name of the LLM model to use
  - Type: String
  - Default: `llama3`
  - Example: `llama3`, `mistral`, `neural-chat`, etc.

- **`llama.stream`**
  - Description: Enable/disable streaming responses
  - Type: Boolean
  - Default: `false`
  - Example: `true` for streaming, `false` for complete response

## Configuration Examples

### Minimal Configuration (only mandatory field)
```yaml
llama:
  prompt: "Translate the following text to the target language: {{text}}"
```
- Uses all defaults: localhost:11434, endpoint /api/generate, model llama3, no streaming

### Local Development
```yaml
llama:
  api:
    url: "http://localhost:11434"
    endpoint: "/api/generate"
  model:
    name: "llama3"
  prompt: "Translate the following text to the target language: {{text}}"
  stream: false
```

### Docker Environment
```yaml
llama:
  api:
    url: "http://host.docker.internal:11434"
    endpoint: "/api/generate"
  model:
    name: "mistral"
  prompt: "You are a helpful translation assistant. Translate: {{text}}"
  stream: true
```

### WSL2 Environment
```yaml
llama:
  api:
    url: "http://172.x.x.x:11434"  # Replace with actual WSL2 IP from: wsl hostname -I
    endpoint: "/api/generate"
  model:
    name: "llama3"
  prompt: "Translate the following text to the target language: {{text}}"
  stream: false
```

### Environment Variables (Spring Boot format)
When using environment variables, Spring Boot converts dots to underscores and uppercases:

```bash
# Linux/Mac
export LLAMA_PROMPT="Translate the following text to the target language: {{text}}"
export LLAMA_API_URL="http://localhost:11434"
export LLAMA_API_ENDPOINT="/api/generate"
export LLAMA_MODEL_NAME="llama3"
export LLAMA_STREAM="false"

# Windows PowerShell
$env:LLAMA_PROMPT = "Translate the following text to the target language: {{text}}"
$env:LLAMA_API_URL = "http://localhost:11434"
$env:LLAMA_API_ENDPOINT = "/api/generate"
$env:LLAMA_MODEL_NAME = "llama3"
$env:LLAMA_STREAM = "false"
```

### Java System Properties
```bash
java -Dllama.prompt="Translate the following text to the target language: {{text}}" \
     -Dllama.api.url="http://localhost:11434" \
     -Dllama.api.endpoint="/api/generate" \
     -Dllama.model.name="llama3" \
     -Dllama.stream=false \
     -jar build/libs/tinglish-api.jar
```

## Validation Behavior

### Validation Framework
- Uses **Jakarta Bean Validation** (JSR-380) via `spring-boot-starter-validation`
- Validation is triggered during Spring configuration binding (`@EnableConfigurationProperties`)
- Validation errors fail fast at application startup (no partial configuration)

### Validation Rules
1. **`prompt` field**: `@NotBlank`
   - Must not be null
   - Must not be empty string (`""`)
   - Must not be only whitespace (`"   "`)
   - Custom message: `"llama.prompt must not be blank or null"`

2. **`model` (nested object)**: Optional, defaults to `Model("llama3")` if missing
3. **`api` (nested object)**: Optional, defaults to `Api("http://localhost:11434", "/api/generate")` if missing

### Error Handling Examples

**Error Case 1: Missing prompt in YAML**
```yaml
llama:
  model:
    name: "llama3"
  # ❌ prompt is missing
```
- Error at startup:
  ```
  ConstraintViolationException: llama.prompt must not be blank or null
  ```

**Error Case 2: Blank prompt in YAML**
```yaml
llama:
  prompt: ""  # ❌ Empty string
  model:
    name: "llama3"
```
- Error at startup:
  ```
  ConstraintViolationException: llama.prompt must not be blank or null
  ```

**Error Case 3: Whitespace-only prompt in YAML**
```yaml
llama:
  prompt: "   "  # ❌ Only spaces
  model:
    name: "llama3"
```
- Error at startup:
  ```
  ConstraintViolationException: llama.prompt must not be blank or null
  ```

**Valid Case: Proper prompt**
```yaml
llama:
  prompt: "Translate the following text to the target language: {{text}}"
  model:
    name: "llama3"
```
- ✅ Application starts successfully

## Usage in TranslationService

The `TranslationService` injects `LlamaRequest`:

```java
@Service
public class TranslationService {
    private final LlamaRequest llamaRequest;

    public TranslationService(WebClient webClient, LlamaRequest llamaRequest) {
        this.llamaRequest = llamaRequest;
    }

    // Use defaults from config
    public String callModel(String prompt) {
        String model = llamaRequest.model().name();
        String defaultPrompt = llamaRequest.prompt();
        String effectivePrompt = (prompt == null || prompt.isBlank()) ? defaultPrompt : prompt;
        // ...
    }

    // Dynamic override at runtime
    public String callModel(String prompt, String model, String fullUrl, boolean stream) {
        // ...
    }
}
```

## Troubleshooting

### Application fails to start with validation error
1. Check `application-local.yaml` for `llama.prompt` entry
2. Ensure the value is not empty or whitespace-only
3. Verify YAML indentation is correct (two spaces, not tabs)
4. Check for typos in the property name (must be `llama.prompt`, not `llama-prompt` or `llama_prompt`)

### Configuration not being picked up
1. Verify the property name matches exactly: `llama.api.url`, `llama.api.endpoint`, etc.
2. Check that `@ConfigurationProperties(prefix = "llama")` is present in `LlamaRequest.java`
3. Verify that `@EnableConfigurationProperties(LlamaRequest.class)` is present in `TinglishApiApplication.java`
4. For environment variables, ensure correct format: `LLAMA_PROMPT`, `LLAMA_API_URL`, etc.

### Default values not being applied
- If you provide `llama.model` but not `llama.model.name`, the field will be null (default is applied only if the entire nested object is missing)
- To use defaults, either omit the entire nested section or provide all required fields

## Best Practices

1. **Always provide `llama.prompt`** - Make it specific to your use case
2. **Use environment-specific YAML files**:
   - `application.yaml` - Default values
   - `application-local.yaml` - Local development
   - `application-prod.yaml` - Production (set active profile in environment)
3. **Environment variables for secrets**: Use env vars for sensitive data (API keys, URLs with auth, etc.)
4. **Document your prompts**: Add comments explaining what each prompt template does
5. **Test configuration at startup**: The validation ensures config is valid before the app fully starts

## Record Structure (for developers)

```java
@ConfigurationProperties(prefix = "llama")
@Validated
public record LlamaRequest(
    @NotBlank(message = "llama.prompt must not be blank or null") 
    String prompt, 
    Model model, 
    Api api, 
    boolean stream) {

    public LlamaRequest {
        if (model == null) model = new Model("llama3");
        if (api == null) api = new Api("http://localhost:11434", "/api/generate");
    }

    public record Model(String name) {}
    public record Api(String url, String endpoint) {}
}
```

- Immutable record (no setters, all fields final)
- Compact constructor provides defaults for nested objects
- Jakarta validation annotations enforce business rules
- Uses Spring `@ConfigurationProperties` for YAML binding

