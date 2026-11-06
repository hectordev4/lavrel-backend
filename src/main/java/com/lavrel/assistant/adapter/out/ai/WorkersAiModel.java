package com.lavrel.assistant.adapter.out.ai;

import com.lavrel.assistant.application.port.out.LanguageModelPort;
import com.lavrel.assistant.domain.AssistantUnavailableException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cloudflare Workers AI through its OpenAI-compatible chat endpoint. CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_AI_TOKEN
 * (a token limited to Workers AI) come from the environment; the model from app.assistant.model
 * (APP_ASSISTANT_MODEL). Cloudflare retires models (410), so check its catalog if answers stop. On the free plan
 * Cloudflare answers 429 when the daily allowance is used up, which becomes a 503 for the player, never a bill.
 */
@Component
public class WorkersAiModel implements LanguageModelPort {

    private static final Logger log = LoggerFactory.getLogger(WorkersAiModel.class);

    private final String accountId;
    private final String token;
    private final String modelName;
    private final RestClient http;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public WorkersAiModel(@Value("${app.assistant.cloudflare-account-id:${CLOUDFLARE_ACCOUNT_ID:}}") String accountId,
                          @Value("${app.assistant.cloudflare-token:${CLOUDFLARE_AI_TOKEN:}}") String token,
                          @Value("${app.assistant.model:@cf/meta/llama-3.1-8b-instruct-fp8}") String modelName) {
        this.accountId = accountId.trim();
        this.token = token.trim();
        this.modelName = modelName;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String modelName() {
        return modelName;
    }

    @Override
    public String complete(String system, String user, int maxTokens) {
        if (accountId.isEmpty() || token.isEmpty()) {
            throw new AssistantUnavailableException("The assistant is not configured", null);
        }
        Map<String, Object> body = Map.of(
                "model", modelName,
                "max_tokens", maxTokens,
                "temperature", 0.3,
                "messages", List.of(Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)));
        try {
            Reply reply = http.post()
                    .uri("https://api.cloudflare.com/client/v4/accounts/{account}/ai/v1/chat/completions", accountId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.writeValueAsString(body))
                    .exchange((request, response) -> new Reply(response.getStatusCode().value(),
                            new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
            return text(reply);
        } catch (RestClientException e) {
            throw new AssistantUnavailableException("The model did not answer", e);
        }
    }

    private String text(Reply reply) {
        if (reply.status() == 429) {
            throw new AssistantUnavailableException("The free daily AI allowance is used up", null);
        }
        if (reply.status() == 404 || reply.status() == 410) {
            log.error("Workers AI does not offer the model '{}' (anymore): set app.assistant.model "
                    + "(APP_ASSISTANT_MODEL) to one from https://developers.cloudflare.com/workers-ai/models/ . {}",
                    modelName, abbreviate(reply.body()));
            throw new AssistantUnavailableException("The configured model is not available", null);
        }
        if (reply.status() >= 400) {
            log.warn("Workers AI answered {}: {}", reply.status(), abbreviate(reply.body()));
            throw new AssistantUnavailableException("The model refused the request", null);
        }
        String text = extract(reply.body());
        if (text == null || text.isBlank()) {
            throw new AssistantUnavailableException("The model gave an empty answer", null);
        }
        return text.trim();
    }

    /** choices[0].message.content (OpenAI format), or result.response (Workers AI's own format). */
    @SuppressWarnings("unchecked")
    String extract(String json) {
        try {
            Map<String, Object> root = mapper.readValue(json, Map.class);
            Object choices = root.get("choices");
            if (choices instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first
                    && first.get("message") instanceof Map<?, ?> message && message.get("content") instanceof String s) {
                return s;
            }
            if (root.get("result") instanceof Map<?, ?> result && result.get("response") instanceof String s) {
                return s;
            }
            return null;
        } catch (RuntimeException e) {
            throw new AssistantUnavailableException("The model's answer could not be read", e);
        }
    }

    private static String abbreviate(String s) {
        return s == null ? "" : s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }

    private record Reply(int status, String body) {
    }
}
