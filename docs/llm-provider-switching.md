# LLM provider switching (OpenAI-compatible)

The backend's LLM integration lives entirely behind `com.legalhelp.common.llm.LlmClient`
(`generate()` for petition drafting, `streamChat()` for interactive chat). The only
implementation is `OpenAiCompatibleLlmClient`, which speaks the standard
`POST /chat/completions` wire format used by OpenAI and every OpenAI-compatible provider —
Ollama, Groq, OpenRouter, LM Studio, Together AI, DeepInfra, and others.

**Switching providers is a configuration change only — no Java code changes, no rebuild
required beyond picking up new config.** Everything is controlled by `app.llm.*` in
`application.yml`, which is itself sourced from environment variables so a deployment only
needs to set env vars.

## Configuration reference

```yaml
app:
  llm:
    provider: openai            # free-text label; only "openrouter" has special behavior (see below)
    base-url: https://api.openai.com/v1
    api-key: sk-xxxx
    model: gpt-4.1
    temperature: 0.3             # optional, default 0.3
    connect-timeout: 10s         # optional, default 10s — TCP/TLS connect timeout
    request-timeout: 60s         # optional, default 60s — time to receive the full response
    retry:
      max-attempts: 3            # optional, default 3 — includes the first, non-retry attempt
      initial-backoff: 500ms     # optional, default 500ms
      backoff-multiplier: 2.0    # optional, default 2.0 — exponential backoff
    open-router:
      referer:                   # optional; sent as HTTP-Referer, only when provider=openrouter
      title:                     # optional; sent as X-Title, only when provider=openrouter
```

Every field except `base-url` and `model` has a default, and the whole `retry` / `open-router`
block may be omitted entirely.

Environment variables consumed by `application.yml` (see `deploy/.env.example`):

| Variable | Purpose |
|---|---|
| `LLM_PROVIDER` | Provider label (`openai`, `groq`, `openrouter`, `ollama`, ...) |
| `LLM_BASE_URL` | OpenAI-compatible base URL, e.g. `https://api.groq.com/openai/v1` |
| `LLM_API_KEY` | Bearer token |
| `LLM_MODEL` | Provider-specific model id |
| `LLM_OPENROUTER_REFERER` / `LLM_OPENROUTER_TITLE` | OpenRouter attribution headers (ignored elsewhere) |

## Provider examples

Only `LLM_BASE_URL`, `LLM_MODEL`, and `LLM_API_KEY` change between providers.

### OpenAI

```bash
LLM_PROVIDER=openai
LLM_BASE_URL=https://api.openai.com/v1
LLM_API_KEY=sk-xxxx
LLM_MODEL=gpt-4.1
```

### Groq

```bash
LLM_PROVIDER=groq
LLM_BASE_URL=https://api.groq.com/openai/v1
LLM_API_KEY=gsk_xxxx
LLM_MODEL=llama-3.3-70b-versatile
```

### OpenRouter

```bash
LLM_PROVIDER=openrouter
LLM_BASE_URL=https://openrouter.ai/api/v1
LLM_API_KEY=sk-or-xxxx
LLM_MODEL=anthropic/claude-3.5-sonnet
LLM_OPENROUTER_REFERER=https://your-app.example.com
LLM_OPENROUTER_TITLE=Legal Help Platform
```

The `HTTP-Referer` and `X-Title` headers are only attached when `LLM_PROVIDER=openrouter`
(case-insensitive) *and* the corresponding value is non-blank.

### Ollama (local)

```bash
LLM_PROVIDER=ollama
LLM_BASE_URL=http://localhost:11434/v1
LLM_API_KEY=dummy
LLM_MODEL=qwen3:8b
```

Ollama's OpenAI-compatible endpoint doesn't validate the API key, but the client always sends
`Authorization: Bearer <value>`, so any non-null placeholder works.

### LM Studio (local)

```bash
LLM_PROVIDER=lmstudio
LLM_BASE_URL=http://localhost:1234/v1
LLM_API_KEY=dummy
LLM_MODEL=<model loaded in LM Studio>
```

### Together AI / DeepInfra

Same pattern — set `LLM_BASE_URL` to the provider's OpenAI-compatible base path (e.g.
`https://api.together.xyz/v1`, `https://api.deepinfra.com/v1/openai`), `LLM_API_KEY`, and
`LLM_MODEL` to the provider's model id.

## Design notes

- **No OpenAI SDK.** Requests are built and parsed by hand with `java.net.http.HttpClient` and
  Jackson (`OpenAiRequest` / `OpenAiResponse` / `OpenAiMessage` / `OpenAiChoice` / `OpenAiDelta`
  in `common.llm.openai.dto`), so any provider that speaks the same wire format works without a
  new client library.
- **Connection pooling.** `OpenAiCompatibleLlmClient` is a Spring singleton holding one shared
  `HttpClient`; the JDK client keeps connections alive and pools them per destination for the
  lifetime of that instance. The client's executor is a virtual-thread-per-task executor, which
  scales to high concurrent request volume.
- **Retries.** `RetryExecutor` retries `generate()` (the whole request/response exchange) and
  the initial connect-and-read-headers step of `streamChat()` on HTTP 429/502/503/504, with
  exponential backoff. Once any part of a stream has been delivered to the caller's
  `LlmStreamHandler`, it is never retried — that would risk replaying tokens already seen by the
  caller. Streaming failures after that point are reported via `handler.onError(...)`.
- **Streaming.** `streamChat()` reads the response body line-by-line
  (`HttpResponse.BodyHandlers.ofLines()`) and never buffers the full response. `StreamingEventParser`
  parses each SSE `data: {...}` line, ignoring blank/comment lines and the terminal
  `data: [DONE]` line.
- **Logging.** Only operation name, duration, provider, model, and HTTP status codes are logged.
  Prompts, conversation history, and the API key are never logged (see
  `OpenAiCompatibleLlmClient#logDuration` and `#extractErrorMessage`).
- **One deliberate behavior change vs. the previous Anthropic-specific client:** the old
  implementation used different `max_tokens` budgets for drafting (8000) vs. chat (2048). The
  OpenAI-compatible request body in this task's spec is exactly `{model, messages, temperature,
  stream}` with no `max_tokens`, so that per-call token budgeting was dropped rather than
  reintroduced as an unrequested field. If per-call token limits are needed, add an optional
  `maxTokens` field to `OpenAiRequest` and a corresponding `app.llm.*` property.

## Testing

- `StreamingEventParserTest`, `RetryExecutorTest` — pure unit tests, no I/O.
- `OpenAiCompatibleLlmClientTest` — runs `OpenAiCompatibleLlmClient` against a real local HTTP
  server (JDK's built-in `com.sun.net.httpserver.HttpServer`, not a mocking framework or the
  OpenAI SDK), covering: successful completion, empty content, HTTP 401 (no retry), HTTP 429
  (retries then succeeds), HTTP 500 (no retry — not in the retryable set), malformed JSON,
  history-to-message conversion and ordering, streaming token-by-token delivery, the `[DONE]`
  sentinel, a malformed streaming chunk (`onError`), an initial-connect HTTP failure during
  streaming (`onError`, no retry semantics violated), and a streaming request that retries its
  initial connect on HTTP 503 before streaming successfully.

Run just this suite:

```bash
cd backend && ./mvnw test -Dtest=StreamingEventParserTest,RetryExecutorTest,OpenAiCompatibleLlmClientTest
```
