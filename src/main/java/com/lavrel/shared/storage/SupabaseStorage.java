package com.lavrel.shared.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Supabase Storage through its REST API, shared by every feature that keeps files (avatars, achievement icons). The
 * buckets must exist and be public. SUPABASE_URL is the project's address and SUPABASE_SERVICE_KEY the service-role
 * key: a secret that bypasses row level security, so it only lives in the backend's environment.
 */
@Component
public class SupabaseStorage {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorage.class);

    private final String baseUrl;
    private final String serviceKey;
    private final RestClient http = RestClient.builder().build();

    public SupabaseStorage(@Value("${app.avatars.storage-url:${SUPABASE_URL:}}") String baseUrl,
                           @Value("${app.avatars.service-key:${SUPABASE_SERVICE_KEY:}}") String serviceKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.serviceKey = serviceKey;
    }

    public boolean configured() {
        return !baseUrl.isBlank() && !serviceKey.isBlank();
    }

    /** Creates or replaces the file. @throws StorageUnavailableException if not configured or refused */
    public void store(String bucket, String path, byte[] bytes, String contentType) {
        if (!configured()) {
            throw new StorageUnavailableException("File storage is not configured", null);
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
            throw new StorageUnavailableException("File storage did not accept the file", e);
        }
    }

    /** Best effort: a failure is logged, not thrown. */
    public void delete(String bucket, String path) {
        if (!configured()) {
            return;
        }
        try {
            http.delete().uri(baseUrl + "/storage/v1/object/" + bucket + "/" + path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceKey)
                    .header("apikey", serviceKey)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Could not delete {}/{}: {}", bucket, path, e.toString());
        }
    }

    public String publicUrl(String bucket, String path) {
        return baseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }
}
