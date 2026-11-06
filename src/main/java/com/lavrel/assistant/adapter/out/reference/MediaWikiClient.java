package com.lavrel.assistant.adapter.out.reference;

import com.lavrel.assistant.application.port.out.WikiClientPort;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

/**
 * The public MediaWiki API (action=query&list=search and action=parse&prop=wikitext). Only addresses an admin
 * approved reach here (WikiSource checks them); redirects are not followed to other hosts. Any failure means "no
 * reference" and the answer simply falls back to the model's own knowledge.
 */
@Component
public class MediaWikiClient implements WikiClientPort {

    private static final Logger log = LoggerFactory.getLogger(MediaWikiClient.class);
    private static final int MAX_BODY_CHARS = 2_000_000;

    private final RestClient http;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public MediaWikiClient() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.http = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.USER_AGENT, "LavrelBot/1.0 (achievement guides; https://lavrel.onrender.com)")
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> search(String apiUrl, String text, int limit) {
        Map<String, Object> root = get(apiUrl
                + "?action=query&list=search&format=json&formatversion=2&srlimit={limit}&srsearch={text}", limit, text);
        if (root.get("query") instanceof Map<?, ?> query && query.get("search") instanceof List<?> hits) {
            return hits.stream()
                    .filter(h -> h instanceof Map<?, ?> m && m.get("title") instanceof String)
                    .map(h -> (String) ((Map<String, Object>) h).get("title"))
                    .toList();
        }
        return List.of();
    }

    @Override
    public Optional<String> wikitext(String apiUrl, String title) {
        Map<String, Object> root = get(apiUrl
                + "?action=parse&prop=wikitext&format=json&formatversion=2&redirects=1&page={title}", title);
        if (root.get("parse") instanceof Map<?, ?> parse && parse.get("wikitext") instanceof String wikitext) {
            return Optional.of(wikitext);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String urlTemplate, Object... variables) {
        try {
            String body = http.get().uri(urlTemplate, variables)
                    .exchange((request, response) -> response.getStatusCode().is2xxSuccessful()
                            ? new String(response.getBody().readNBytes(MAX_BODY_CHARS), StandardCharsets.UTF_8)
                            : null);
            return body == null ? Map.of() : mapper.readValue(body, Map.class);
        } catch (RestClientException | IllegalArgumentException e) {
            log.warn("Wiki request failed: {}", e.toString());
            return Map.of();
        } catch (RuntimeException e) {
            log.warn("Wiki answer could not be read: {}", e.toString());
            return Map.of();
        }
    }
}
