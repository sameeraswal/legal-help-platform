package com.legalhelp.common.llm.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalhelp.common.llm.LlmException;
import com.legalhelp.common.llm.LlmHttpException;
import com.legalhelp.common.llm.LlmMessage;
import com.legalhelp.common.llm.LlmRole;
import com.legalhelp.common.llm.LlmStreamHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Exercises {@link OpenAiCompatibleLlmClient} end-to-end against a real, local HTTP server
 * (JDK's built-in {@link HttpServer} — no mocking framework, no OpenAI SDK), verifying the client
 * behaves correctly regardless of which OpenAI-compatible provider is on the other end.
 */
class OpenAiCompatibleLlmClientTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer server;
    private ExecutorService serverExecutor;
    private final AtomicReference<HttpHandler> handler = new AtomicReference<>();
    private final AtomicInteger requestCount = new AtomicInteger();
    private OpenAiCompatibleLlmClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            requestCount.incrementAndGet();
            handler.get().handle(exchange);
        });
        serverExecutor = Executors.newCachedThreadPool();
        server.setExecutor(serverExecutor);
        server.start();

        OpenAiConfigurationProperties config = new OpenAiConfigurationProperties(
                "openai",
                "http://localhost:" + server.getAddress().getPort(),
                "test-api-key",
                "test-model",
                0.3,
                Duration.ofSeconds(5),
                Duration.ofSeconds(5),
                new OpenAiConfigurationProperties.Retry(3, Duration.ofMillis(5), 2.0),
                null);
        client = new OpenAiCompatibleLlmClient(config);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        serverExecutor.shutdownNow();
    }

    // ---------------------------------------------------------------------
    // generate()
    // ---------------------------------------------------------------------

    @Test
    void generate_returnsContent_onSuccessfulCompletion() {
        handler.set((HttpExchange exchange) -> respondJson(exchange, 200, """
                {"id":"cmpl-1","model":"test-model","choices":[
                    {"index":0,"message":{"role":"assistant","content":"Hello there"},"finish_reason":"stop"}
                ]}
                """));

        String result = client.generate("You are helpful.", List.of(), "Hi");

        assertThat(result).isEqualTo("Hello there");
        assertThat(requestCount.get()).isEqualTo(1);
    }

    @Test
    void generate_throwsLlmException_onEmptyResponseContent() {
        handler.set((HttpExchange exchange) -> respondJson(exchange, 200, """
                {"id":"cmpl-1","choices":[
                    {"index":0,"message":{"role":"assistant","content":""},"finish_reason":"content_filter"}
                ]}
                """));

        assertThatThrownBy(() -> client.generate("system", List.of(), "Hi"))
                .isInstanceOf(LlmException.class)
                .isNotInstanceOf(LlmHttpException.class)
                .hasMessageContaining("content_filter");
    }

    @Test
    void generate_throwsLlmHttpException_onUnauthorized() {
        handler.set((HttpExchange exchange) -> respondJson(exchange, 401, """
                {"error":{"message":"Invalid API key"}}
                """));

        assertThatThrownBy(() -> client.generate("system", List.of(), "Hi"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.throwable(LlmHttpException.class))
                .extracting(LlmHttpException::statusCode)
                .isEqualTo(401);
        // 401 is not retryable -> exactly one attempt
        assertThat(requestCount.get()).isEqualTo(1);
    }

    @Test
    void generate_retriesOn429ThenSucceeds() {
        handler.set((HttpExchange exchange) -> {
            if (requestCount.get() < 3) {
                respondJson(exchange, 429, "{\"error\":{\"message\":\"rate limited\"}}");
            } else {
                respondJson(exchange, 200, """
                        {"choices":[{"index":0,"message":{"role":"assistant","content":"recovered"},"finish_reason":"stop"}]}
                        """);
            }
        });

        String result = client.generate("system", List.of(), "Hi");

        assertThat(result).isEqualTo("recovered");
        assertThat(requestCount.get()).isEqualTo(3);
    }

    @Test
    void generate_doesNotRetry_onInternalServerError() {
        handler.set((HttpExchange exchange) -> respondJson(exchange, 500, "{\"error\":{\"message\":\"boom\"}}"));

        assertThatThrownBy(() -> client.generate("system", List.of(), "Hi"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.throwable(LlmHttpException.class))
                .extracting(LlmHttpException::statusCode)
                .isEqualTo(500);
        // 500 is not in the retryable set (429, 502, 503, 504) -> exactly one attempt
        assertThat(requestCount.get()).isEqualTo(1);
    }

    @Test
    void generate_throwsLlmException_onMalformedJson() {
        handler.set((HttpExchange exchange) -> respondJson(exchange, 200, "{not-valid-json"));

        assertThatThrownBy(() -> client.generate("system", List.of(), "Hi"))
                .isInstanceOf(LlmException.class)
                .isNotInstanceOf(LlmHttpException.class)
                .hasMessageContaining("Malformed JSON");
    }

    @Test
    void generate_convertsHistoryIntoOrderedOpenAiMessages() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        handler.set((HttpExchange exchange) -> {
            capturedBody.set(readBody(exchange));
            respondJson(exchange, 200, """
                    {"choices":[{"index":0,"message":{"role":"assistant","content":"ok"},"finish_reason":"stop"}]}
                    """);
        });

        client.generate(
                "You are a legal assistant.",
                List.of(
                        new LlmMessage(LlmRole.USER, "Hi"),
                        new LlmMessage(LlmRole.ASSISTANT, "Hello! How can I help?")),
                "What's the weather?");

        JsonNode root = JSON.readTree(capturedBody.get());
        assertThat(root.path("model").asText()).isEqualTo("test-model");
        assertThat(root.path("temperature").asDouble()).isEqualTo(0.3);
        assertThat(root.path("stream").asBoolean()).isFalse();

        JsonNode messages = root.path("messages");
        assertThat(messages).hasSize(4);
        assertThat(messages.get(0).path("role").asText()).isEqualTo("system");
        assertThat(messages.get(0).path("content").asText()).isEqualTo("You are a legal assistant.");
        assertThat(messages.get(1).path("role").asText()).isEqualTo("user");
        assertThat(messages.get(1).path("content").asText()).isEqualTo("Hi");
        assertThat(messages.get(2).path("role").asText()).isEqualTo("assistant");
        assertThat(messages.get(2).path("content").asText()).isEqualTo("Hello! How can I help?");
        assertThat(messages.get(3).path("role").asText()).isEqualTo("user");
        assertThat(messages.get(3).path("content").asText()).isEqualTo("What's the weather?");
    }

    @Test
    void generate_omitsSystemMessage_whenSystemPromptIsBlank() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        handler.set((HttpExchange exchange) -> {
            capturedBody.set(readBody(exchange));
            respondJson(exchange, 200, """
                    {"choices":[{"index":0,"message":{"role":"assistant","content":"ok"},"finish_reason":"stop"}]}
                    """);
        });

        client.generate("   ", List.of(), "Hi");

        JsonNode messages = JSON.readTree(capturedBody.get()).path("messages");
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).path("role").asText()).isEqualTo("user");
    }

    // ---------------------------------------------------------------------
    // streamChat()
    // ---------------------------------------------------------------------

    @Test
    void streamChat_emitsTokensInOrder_andCompletesOnDone() {
        handler.set((HttpExchange exchange) -> respondSse(exchange, String.join("\n",
                "data: {\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\"}}]}",
                "",
                "data: {\"choices\":[{\"index\":0,\"delta\":{\"content\":\"Hel\"}}]}",
                "",
                "data: {\"choices\":[{\"index\":0,\"delta\":{\"content\":\"lo\"}}]}",
                "",
                "data: [DONE]",
                "")));
        RecordingStreamHandler recorder = new RecordingStreamHandler();

        client.streamChat("system", List.of(), "Hi", recorder);

        awaitTerminal(recorder);
        assertThat(recorder.tokens).containsExactly("Hel", "lo");
        assertThat(recorder.completed).isTrue();
        assertThat(recorder.error).isNull();
    }

    @Test
    void streamChat_callsOnError_onMalformedChunk() {
        handler.set((HttpExchange exchange) -> respondSse(exchange, String.join("\n",
                "data: {\"choices\":[{\"index\":0,\"delta\":{\"content\":\"partial\"}}]}",
                "",
                "data: {not-valid-json",
                "")));
        RecordingStreamHandler recorder = new RecordingStreamHandler();

        client.streamChat("system", List.of(), "Hi", recorder);

        awaitTerminal(recorder);
        assertThat(recorder.tokens).containsExactly("partial");
        assertThat(recorder.error).isInstanceOf(LlmException.class);
        assertThat(recorder.completed).isFalse();
    }

    @Test
    void streamChat_callsOnError_onInitialHttpFailure() {
        handler.set((HttpExchange exchange) -> respondJson(exchange, 500, "{\"error\":{\"message\":\"down\"}}"));
        RecordingStreamHandler recorder = new RecordingStreamHandler();

        client.streamChat("system", List.of(), "Hi", recorder);

        awaitTerminal(recorder);
        assertThat(recorder.tokens).isEmpty();
        assertThat(recorder.completed).isFalse();
        assertThat(recorder.error).isInstanceOf(LlmHttpException.class);
        assertThat(((LlmHttpException) recorder.error).statusCode()).isEqualTo(500);
    }

    @Test
    void streamChat_retriesInitialConnectOn503_thenStreamsSuccessfully() {
        handler.set((HttpExchange exchange) -> {
            if (requestCount.get() < 2) {
                respondJson(exchange, 503, "{\"error\":{\"message\":\"warming up\"}}");
            } else {
                respondSse(exchange, String.join("\n",
                        "data: {\"choices\":[{\"index\":0,\"delta\":{\"content\":\"ready\"}}]}",
                        "",
                        "data: [DONE]",
                        ""));
            }
        });
        RecordingStreamHandler recorder = new RecordingStreamHandler();

        client.streamChat("system", List.of(), "Hi", recorder);

        awaitTerminal(recorder);
        assertThat(recorder.tokens).containsExactly("ready");
        assertThat(recorder.completed).isTrue();
        assertThat(requestCount.get()).isEqualTo(2);
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private static void awaitTerminal(RecordingStreamHandler recorder) {
        await().atMost(Duration.ofSeconds(5)).until(() -> recorder.completed || recorder.error != null);
    }

    private static void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void respondSse(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static final class RecordingStreamHandler implements LlmStreamHandler {
        final List<String> tokens = new ArrayList<>();
        volatile boolean completed = false;
        volatile Throwable error;

        @Override
        public void onToken(String textDelta) {
            tokens.add(textDelta);
        }

        @Override
        public void onComplete() {
            completed = true;
        }

        @Override
        public void onError(Throwable error) {
            this.error = error;
        }
    }
}
