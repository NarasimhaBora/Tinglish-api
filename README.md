# Tinglish API - Complete Documentation

**Tinglish API** is a Spring Boot-based REST API that converts Tinglish (Telugu written in English script) into grammatically correct Telugu script using AI-powered language models through the OLLAMA API. The application employs a multi-pass AI orchestration strategy to ensure accurate translations and grammar corrections.

---

## 🏗️ Architecture

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                          CLIENT REQUEST                             │
│         POST /api/tinglish-service/v1/translations                  │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
                           ▼
        ┌──────────────────────────────────┐
        │  TranslationController            │
        │  - Validates incoming request     │
        │  - Handles REST API endpoints     │
        │  - Returns Flux<String> response  │
        └────────────────┬──────────────────┘
                         │
                         ▼
        ┌──────────────────────────────────┐
        │   AiOrchestrator                  │
        │   - Multi-pass translation        │
        │   - Context management            │
        │   - Pass 1: Grammar correction    │
        │   - Pass 2: Refinement            │
        │   - Pass 3: Validation            │
        │   - Pass 4: Consistency check     │
        │   - English leakage detection     │
        └────────────────┬──────────────────┘
                         │
          ┌──────────────┼──────────────┐
          │              │              │
          ▼              ▼              ▼
    ┌──────────┐  ┌──────────────┐  ┌──────────┐
    │PromptBuilder TranslationService WebClientConfig
    │            │                │
    │ - Pass 1   │ - API calls    │ - HTTP config
    │ - Pass 2   │ - Model select │ - Timeouts
    │ - Pass 3   │ - Streaming    │ - Reactor Netty
    │ - Pass 4   │ - Config binding
    └──────────┘  └──────────────┘  └──────────┘
                         │
                         ▼
        ┌──────────────────────────────────┐
        │  OLLAMA API (Local LLM)           │
        │  http://localhost:11434/api/generate
        └──────────────────────────────────┘
```

### Layered Architecture

```
┌─────────────────────────────────────────┐
│      REST API Layer (Controller)         │
│   - TranslationController                │
│   - Input validation                     │
│   - REST endpoint mapping                │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│      Service Layer                       │
│   - AiOrchestrator (Orchestration)       │
│   - TranslationService (API calls)       │
│   - PromptBuilder (Prompt generation)    │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│      Configuration Layer                 │
│   - WebClientConfig (HTTP client)        │
│   - LlamaRequest (Config properties)     │
│   - Spring Boot Configuration            │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│      Data Transfer Layer (DTOs)          │
│   - TranslationRequest                   │
│   - LlamaRequest (Config POJO)           │
│   - Nested records (Model, Api)          │
└─────────────────────────────────────────┘
```

---

## 🔄 Data Flow & Translation Process

### Request/Response Flow

```
1. CLIENT REQUEST
   ├─ Endpoint: POST http://localhost:8080/api/tinglish-service/v1/translations
   ├─ Content-Type: application/json
   └─ Body:
      {
        "text": "namastey",
        "sourceLanguage": "tinglish",
        "targetLanguage": "telugu"
      }

2. TRANSLATION CONTROLLER
   ├─ Validates TranslationRequest (text, sourceLanguage, targetLanguage)
   ├─ Checks: text not blank, max 5000 characters
   ├─ Forwards to AiOrchestrator.streamTranslation(text)
   └─ Returns: Flux<String> (text/event-stream)

3. AI ORCHESTRATOR - MULTI-PASS PROCESS
   │
   ├─ PASS 1: Grammar Correction & Conversion
   │  ├─ Input: "namastey" + previous context (up to 3 items)
   │  ├─ Prompt: "You are a professional Telugu linguist..."
   │  ├─ Output: "నమస్తే" (corrected Telugu)
   │  └─ Model: LLAMA3 via HTTP POST
   │
   ├─ PASS 2: Grammar Refinement
   │  ├─ Input: Previous pass output
   │  ├─ Prompt: "You are a Telugu grammar perfectionist..."
   │  ├─ Output: "నమస్తే" (refined if needed)
   │  └─ Model: LLAMA3 via HTTP POST
   │
   ├─ PASS 3: Validation
   │  ├─ Input: Previous pass output
   │  ├─ Prompt: "Ensure: No English letters, Natural flow..."
   │  ├─ Output: "నమస్తే" (validated)
   │  └─ Model: LLAMA3 via HTTP POST
   │
   ├─ PASS 4: Consistency Check
   │  ├─ Input: Original "namastey" + Pass 3 output
   │  ├─ Prompt: "Check meaning preservation..."
   │  ├─ Output: "నమస్తే" (consistent)
   │  └─ Model: LLAMA3 via HTTP POST
   │
   ├─ ENGLISH LEAKAGE DETECTION
   │  ├─ Regex check for [a-zA-Z] characters
   │  ├─ If found: Run Pass 2 again
   │  └─ Output: Clean Telugu text
   │
   └─ SESSION CONTEXT MANAGEMENT
      ├─ Add final output to sessionContext list
      ├─ Maintain sliding window: last 3 translations
      └─ Use as reference for next translations

4. TRANSLATION SERVICE
   ├─ Each prompt call invokes callModel()
   ├─ HTTP POST to OLLAMA API
   │  ├─ URL: http://localhost:11434/api/generate
   │  ├─ Body: {"model": "llama3", "prompt": "...", "stream": false}
   │  └─ Headers: Content-Type: application/json
   │
   ├─ Response Parsing
   │  ├─ Extract JSON response
   │  └─ Parse: response.path("response").asText()
   │
   └─ Timeout Handling (via WebClientConfig)
      ├─ Connection timeout: 200 seconds
      ├─ Response timeout: 300 seconds
      ├─ Read timeout: 300 seconds
      └─ Write timeout: 300 seconds

5. RESPONSE TO CLIENT
   ├─ Final translated text: "నమస్తే"
   ├─ Content-Type: text/event-stream
   ├─ Status: 200 OK
   └─ Body: "నమస్తే" (pure Telugu output)
```

---

## 🛠️ Technical Stack

### Core Framework
- **Spring Boot**: 4.0.3
  - Spring MVC & WebFlux (Reactive)
  - Spring Actuator (Health endpoints)
  - Spring Boot Configuration Properties
  - Spring Validation (Jakarta Bean Validation)

### Java Version
- **Java 21** (LTS)
  - Records (used for DTOs and Config POJOs)
  - Virtual threads support (for async operations)

### HTTP & Networking
- **WebClient** (Spring Reactive Web Client)
  - Async, non-blocking HTTP calls
  - Built on Project Reactor (Reactive Streams)
  - Used for OLLAMA API communication

- **Reactor Netty**
  - Underlying HTTP client for WebClient
  - Handles async I/O

- **Project Reactor** (Flux, Mono)
  - Reactive streams library
  - Enables streaming responses

### Data Format
- **Jackson** (JSON processing)
  - Automatic JSON serialization/deserialization
  - YAML parsing for application.yaml config

### Validation
- **Jakarta Bean Validation** (spring-boot-starter-validation)
  - @NotBlank, @Size annotations
  - ConstraintViolation handling
  - Validation for configuration properties

### Build & Dependency Management
- **Gradle 8.x+**
  - Build automation
  - Dependency resolution

- **Maven Central Repository**
  - Artifact resolution

### Networking & Timeouts
- **Netty** (io.netty)
  - ChannelOption for socket configuration
  - ReadTimeoutHandler, WriteTimeoutHandler
  - Advanced TCP/IP configuration

### Configuration
- **YAML Files**
  - application.yaml (default)
  - application-local.yaml (local profile)
  - Spring Profiles: active=local

### Runtime Environment
- **Server**: Embedded Tomcat (Spring Boot default)
  - Port: 8080
  - Context path: /api/tinglish-service
  - Graceful shutdown: enabled

- **External Dependency**: OLLAMA
  - Local LLM service (http://localhost:11434)
  - API endpoint: /api/generate
  - Models: llama3, mistral, neural-chat, etc.

---

## 📂 Project Structure

```
Tinglish-api/
│
├── src/main/java/com/ai/tinglish/
│   │
│   ├── TinglishApiApplication.java
│   │   └── Main Spring Boot application entry point
│   │       @EnableConfigurationProperties(LlamaRequest.class)
│   │
│   ├── controller/
│   │   └── TranslationController.java
│   │       • POST /v1/translations endpoint
│   │       • Accepts: TranslationRequest (text, sourceLanguage, targetLanguage)
│   │       • Returns: Flux<String> (streaming response)
│   │       • Handles CORS
│   │
│   ├── service/
│   │   └── TranslationService.java
│   │       • callModel(prompt): Uses default config properties
│   │       • callModel(prompt, model, fullUrl, stream): Dynamic params
│   │       • Makes HTTP POST calls to OLLAMA API
│   │       • Parses JSON responses
│   │
│   ├── ai/
│   │   ├── AiOrchestrator.java
│   │   │   • Orchestrates multi-pass translation workflow
│   │   │   • Maintains session context (last 3 translations)
│   │   │   • Detects and fixes English character leakage
│   │   │   • Calls PromptBuilder + TranslationService in sequence
│   │   │
│   │   └── PromptBuilder.java
│   │       • buildPass1(input, context): Grammar correction
│   │       • buildPass2(telugu): Refinement
│   │       • buildValidator(telugu): Validation
│   │       • buildConsistency(original, telugu): Consistency check
│   │
│   ├── config/
│   │   └── WebClientConfig.java
│   │       • Configures Spring WebClient bean
│   │       • Sets connection timeout: 200 seconds
│   │       • Sets response timeout: 300 seconds
│   │       • Sets read/write timeouts: 300 seconds
│   │       • Uses Reactor Netty as HTTP client
│   │
│   └── dto/
│       ├── LlamaRequest.java
│       │   • @ConfigurationProperties(prefix = "llama")
│       │   • Nested records: Model, Api
│       │   • Fields: prompt (mandatory), model, api, stream
│       │   • Validation: @NotBlank on prompt
│       │   • Defaults: llama3 model, localhost:11434 URL
│       │
│       └── TranslationRequest.java
│           • text: Input text (mandatory, max 5000 chars)
│           • sourceLanguage: Source language code
│           • targetLanguage: Target language code
│
├── src/main/resources/
│   ├── application.yaml
│   │   • Spring profiles configuration
│   │   • LLAMA API configuration (default)
│   │   • Server configuration
│   │
│   └── application-local.yaml
│       • Local development configuration
│       • H2 in-memory database
│       • Hikari connection pool settings
│       • Hibernatte configuration
│
├── build.gradle
│   • Gradle build configuration
│   • Java 21 language level
│   • Spring Boot 4.0.3
│
└── README.md
    • This file - Complete project documentation
```

---

## 🚀 Getting Started

### Prerequisites
1. **Java 21 JDK**
   ```bash
   java -version  # Should show version 21.x.x
   ```

2. **Gradle** (or use included wrapper)
   ```bash
   gradle --version  # Should show 8.x+
   ```

3. **OLLAMA** (for LLM functionality)
   - Install from: https://ollama.ai
   - Start OLLAMA service: `ollama serve`
   - Pull a model: `ollama pull llama3`
   - Verify running: `curl http://localhost:11434/api/generate`

### Build the Project

```bash
# Using Gradle wrapper (Windows)
./gradlew.bat clean build

# Using Gradle (if installed)
gradle clean build
```

### Run the Application

```bash
# Option 1: Using Gradle
./gradlew.bat bootRun

# Option 2: Using Java directly
java -jar build/libs/Tinglish-api-0.0.1-SNAPSHOT.jar

# Option 3: With custom OLLAMA URL
java -jar build/libs/Tinglish-api-0.0.1-SNAPSHOT.jar \
  --llama.api.url=http://host.docker.internal:11434 \
  --llama.api.endpoint=/api/generate
```

### Default Configuration
- **Server URL**: http://localhost:8080
- **Context Path**: /api/tinglish-service
- **OLLAMA URL**: http://localhost:11434
- **OLLAMA Endpoint**: /api/generate
- **Model**: llama3
- **Streaming**: false (complete response)

---

## 📡 API Endpoints

### Translation Endpoint (Main)

**POST** `/api/tinglish-service/v1/translations`

**Request Body:**
```json
{
  "text": "namastey",
  "sourceLanguage": "tinglish",
  "targetLanguage": "telugu"
}
```

**Response:**
```
Content-Type: text/event-stream
Status: 200 OK

నమస్తే
```

**Validation Rules:**
- `text`: 
  - Required (not blank)
  - Max 5000 characters
  - Error: "Text must not be empty" or "Text exceeds maximum allowed length"
  
- `sourceLanguage`:
  - Required (not blank)
  - Error: "Source language is required"
  
- `targetLanguage`:
  - Required (not blank)
  - Error: "Target language is required"

**CORS:** Enabled for all origins

### Health Check Endpoint (Actuator)

**GET** `/api/tinglish-service/actuator/health`

Response:
```json
{
  "status": "UP",
  "components": {...}
}
```

---

## ⚙️ Configuration

### YAML Configuration Properties

All properties are under `llama` prefix in application.yaml:

```yaml
llama:
  # MANDATORY - Will fail startup if not provided
  prompt: "Your default prompt template"
  
  # Optional - Model configuration
  model:
    name: "llama3"  # Default: llama3
  
  # Optional - API Configuration
  api:
    url: "http://localhost:11434"           # Default URL
    endpoint: "/api/generate"                # Default endpoint
  
  # Optional - Streaming
  stream: false  # Default: false (no streaming)
```

### Property Binding Details

| Property | Type | Required | Default | Notes |
|----------|------|----------|---------|-------|
| `llama.prompt` | String | ✅ Yes | N/A | @NotBlank validation |
| `llama.model.name` | String | ❌ No | llama3 | Model name |
| `llama.api.url` | String | ❌ No | http://localhost:11434 | Base URL |
| `llama.api.endpoint` | String | ❌ No | /api/generate | Endpoint path |
| `llama.stream` | Boolean | ❌ No | false | Streaming flag |

### Local Development Configuration

```yaml
# application-local.yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

llama:
  api:
    url: "http://localhost:11434"
    endpoint: "/api/generate"
  prompt: "Translate the following text to the target language: {{text}}"
  model:
    name: "llama3"
  stream: false
```

### Runtime Configuration Override

```bash
java -jar app.jar \
  --llama.api.url=http://remote-ollama:11434 \
  --llama.model.name=mistral \
  --llama.stream=true
```

---

## 🔧 Advanced Configuration

### WebClient Timeouts

Configured in `WebClientConfig.java`:

```
- Connection Timeout: 200 seconds (socket connection)
- Response Timeout: 300 seconds (total response time)
- Read Timeout: 300 seconds (reading data from socket)
- Write Timeout: 300 seconds (writing data to socket)
```

Adjust for slow OLLAMA models in `WebClientConfig.java`:
```java
.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 300000)  // Increase
.responseTimeout(java.time.Duration.ofSeconds(600))     // Increase
```

---

## 🐛 Troubleshooting

### Issue: "OLLAMA not reachable"
```
Error: Connection refused at http://localhost:11434
```

**Solution:**
1. Start OLLAMA: `ollama serve`
2. Verify connection: `curl http://localhost:11434/api/generate`
3. Check firewall rules

### Issue: "Model not found"
```
Error: model 'llama3' not found
```

**Solution:**
```bash
ollama pull llama3
ollama list  # Verify model is pulled
```

### Issue: Timeout during translation
```
Error: response timeout after 300 seconds
```

**Solution:**
- Increase timeout in WebClientConfig
- Use faster model (neural-chat instead of llama3)
- Check OLLAMA resource usage

### Issue: "llama.prompt must not be blank"
```
ConstraintViolationException during startup
```

**Solution:**
- Ensure application.yaml has `llama.prompt` property
- Prompt cannot be null or empty

---

## 📚 Key Classes Overview

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `TinglishApiApplication` | Spring Boot entry point | main() |
| `TranslationController` | REST API handler | streamTranslation() |
| `AiOrchestrator` | Multi-pass workflow | streamTranslation() |
| `PromptBuilder` | Prompt templates | buildPass1/2/3/4() |
| `TranslationService` | OLLAMA API calls | callModel() (2 overloads) |
| `WebClientConfig` | HTTP client config | translationServiceClient() |
| `LlamaRequest` | Config POJO (Record) | api(), model(), stream(), prompt() |
| `TranslationRequest` | Input DTO (Record) | text(), sourceLanguage(), targetLanguage() |

---

## 🔐 Security Considerations

1. **CORS**: Enabled for all origins (customize if needed)
2. **Input Validation**: Max 5000 chars on text input
3. **Timeout Protection**: Long timeouts (300s) to prevent hanging
4. **No Authentication**: Currently open API (add Spring Security if needed)
5. **SQL Injection**: Uses H2 in-memory DB (not exposed in current impl)

---

## 📖 Reference Documentation

### External Links
* [Spring Boot 4.0.3 Documentation](https://docs.spring.io/spring-boot/4.0.3/reference/)
* [Spring WebClient Guide](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html)
* [Project Reactor Documentation](https://projectreactor.io/docs)
* [Jakarta Bean Validation](https://jakarta.ee/specifications/bean-validation/)
* [OLLAMA Documentation](https://github.com/ollama/ollama)
* [Gradle Documentation](https://docs.gradle.org)

---

## 📝 Notes

- **Java Records**: Used for immutable DTOs (LlamaRequest, TranslationRequest)
- **Reactive Streams**: Flux for streaming responses, Mono for single values
- **Spring Profiles**: `active: local` loads application-local.yaml
- **Graceful Shutdown**: Server waits for in-flight requests to complete
- **Session Context**: Maintains sliding window of last 3 translations for context
- **Multi-Pass Translation**: 4 passes ensure accuracy with leakage detection

---

**Version**: 0.0.1-SNAPSHOT  
**Last Updated**: March 5, 2026  
**Status**: Active Development

