package com.lavrel.catalog.steam;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** GET https://store.steampowered.com/api/appdetails?appids={appid} */
@Component
public class SteamAppDetailsClient {

    public record AppDetails(long appId, String type, List<Integer> contentDescriptorIds, List<String> genres) {
    }

    // Response shape: { "<appid>": { "success": true, "data": { "type", "content_descriptors": { "ids" }, "genres" } } }
    // Field names mirror Steam's JSON, so no Jackson imports are needed (works on Jackson 2 and 3).
    record Envelope(boolean success, Data data) {
    }

    record Data(String type, ContentDescriptors content_descriptors, List<Genre> genres) {
    }

    record ContentDescriptors(List<Integer> ids) {
    }

    record Genre(String id, String description) {
    }

    private static final ParameterizedTypeReference<Map<String, Envelope>> RESPONSE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient http;

    public SteamAppDetailsClient(RestClient.Builder builder) {
        this.http = builder.baseUrl("https://store.steampowered.com").build();
    }

    /**
     * Empty when Steam answers success=false (removed, region-locked or restricted app).
     * Network and HTTP errors are thrown so the caller can treat them as transient.
     */
    public Optional<AppDetails> fetch(long appId) {
        Map<String, Envelope> body = http.get()
                .uri("/api/appdetails?appids={id}&l=english", appId)
                .retrieve()
                .body(RESPONSE);
        Envelope entry = body == null ? null : body.get(String.valueOf(appId));
        if (entry == null || !entry.success() || entry.data() == null) {
            return Optional.empty();
        }
        Data data = entry.data();

        List<Integer> descriptorIds = data.content_descriptors() == null || data.content_descriptors().ids() == null
                ? List.of()
                : data.content_descriptors().ids().stream().filter(id -> id != null).toList();

        List<String> genres = data.genres() == null
                ? List.of()
                : data.genres().stream()
                        .filter(g -> g != null && g.description() != null)
                        .map(Genre::description)
                        .toList();

        return Optional.of(new AppDetails(appId, data.type(), descriptorIds, genres));
    }
}
