package com.lavrel.platform.adapter.out.steam;

import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformUnavailableException;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Small HTTP helper shared by the Steam adapters. It never throws on HTTP error statuses (callers
 * decide what 400/403 mean for each Steam endpoint) and turns network failures into
 * PlatformUnavailableException. The API key is passed as a URI variable and never logged.
 */
@Component
public class SteamHttp {

    /** Status code and raw body of a Steam response. */
    public record Response(int status, String body) {
        public boolean ok() {
            return status >= 200 && status < 300;
        }
    }

    private final RestClient client;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final String apiKey;

    public SteamHttp(@Value("${app.steam.api-key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("app.steam.api-key is empty: set STEAM_API_KEY in your .env");
        }
        this.apiKey = apiKey;
        HttpClient jdkClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdkClient);
        factory.setReadTimeout(Duration.ofSeconds(20));
        this.client = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.USER_AGENT, "lavrel-backend")
                .build();
    }

    public String apiKey() {
        return apiKey;
    }

    public Response get(String urlTemplate, Object... uriVariables) {
        try {
            return client.get()
                    .uri(urlTemplate, uriVariables)
                    .exchange((request, response) ->
                            new Response(response.getStatusCode().value(), read(response.getBody())));
        } catch (RestClientException e) {
            throw new PlatformUnavailableException(Platform.STEAM, "request failed", e);
        }
    }

    public Response postForm(String url, MultiValueMap<String, String> form) {
        try {
            return client.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .exchange((request, response) ->
                            new Response(response.getStatusCode().value(), read(response.getBody())));
        } catch (RestClientException e) {
            throw new PlatformUnavailableException(Platform.STEAM, "request failed", e);
        }
    }

    /** Parses a JSON object body; an empty body is an empty object. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> json(Response response) {
        if (response.body() == null || response.body().isBlank()) {
            return Map.of();
        }
        try {
            return mapper.readValue(response.body(), Map.class);
        } catch (RuntimeException e) {
            throw new PlatformUnavailableException(Platform.STEAM, "response was not valid JSON", e);
        }
    }

    private static String read(InputStream body) throws IOException {
        return new String(body.readAllBytes(), StandardCharsets.UTF_8);
    }
}
