package com.lavrel.profile.adapter.out.storage;

import com.lavrel.profile.application.port.out.AvatarStoragePort;
import com.lavrel.profile.domain.AvatarStorageUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Supabase Storage through its REST API. The bucket (default "avatars") must exist and be public. SUPABASE_URL is the
 * project's address (https://xxxx.supabase.co) and SUPABASE_SERVICE_KEY the service-role key: a secret that bypasses
 * row level security, so it only lives in the backend's environment. Without them uploads answer 503 and everything
 * else keeps working.
 */
@Component
public class SupabaseAvatarStorage implements AvatarStoragePort {

    private static final Logger log = LoggerFactory.getLogger(SupabaseAvatarStorage.class);

    private final String baseUrl;
    private final String serviceKey;
    private final String bucket;
    private final RestClient http;

    public SupabaseAvatarStorage(@Value("${app.avatars.storage-url:${SUPABASE_URL:}}") String baseUrl,
                                 @Value("${app.avatars.service-key:${SUPABASE_SERVICE_KEY:}}") String serviceKey,
                                 @Value("${app.avatars.bucket:avatars}") String bucket) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.serviceKey = serviceKey;
        this.bucket = bucket;
        this.http = RestClient.builder().build();
    }

    @Override
    public void store(String path, byte[] bytes, String contentType) {
        if (baseUrl.isBlank() || serviceKey.isBlank()) {
            throw new AvatarStorageUnavailableException("Picture storage is not configured", null);
        }
        try {
            http.post().uri(baseUrl + "/storage/v1/object/" + bucket + "/" + path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceKey)
                    .header("apikey", serviceKey)
                    .header("x-upsert", "true")
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new AvatarStorageUnavailableException("Picture storage did not accept the upload", e);
        }
    }

    @Override
    public void delete(String path) {
        if (baseUrl.isBlank() || serviceKey.isBlank()) {
            return;
        }
        try {
            http.delete().uri(baseUrl + "/storage/v1/object/" + bucket + "/" + path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceKey)
                    .header("apikey", serviceKey)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Could not delete the old picture {}: {}", path, e.toString());
        }
    }

    @Override
    public String publicUrl(String path) {
        return baseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }
}
