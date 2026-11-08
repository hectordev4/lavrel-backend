package com.lavrel.catalog.adapter.out.images;

import com.lavrel.catalog.application.port.out.IconFilesPort;
import com.lavrel.shared.storage.SupabaseStorage;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Downloads icons from Steam's image hosts (the configured one first, then the others Steam has used) and keeps them in
 * our storage under the same "{appId}/{file}" path. Only those fixed hosts are ever contacted, and only for paths of
 * the form "digits/file.jpg|png|gif", so this cannot be pointed anywhere else.
 */
@Component
public class SteamIconFiles implements IconFilesPort {

    static final Pattern ICON_PATH = Pattern.compile("[0-9]+/[A-Za-z0-9_.-]+\\.(jpg|jpeg|png|gif)");
    static final List<String> KNOWN_HOSTS = List.of(
            "https://shared.fastly.steamstatic.com/community_assets/images/apps/",
            "https://shared.akamai.steamstatic.com/community_assets/images/apps/",
            "https://cdn.akamai.steamstatic.com/steamcommunity/public/images/apps/",
            "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/apps/");
    private static final int MAX_BYTES = 512 * 1024;

    private final SupabaseStorage storage;
    private final String bucket;
    private final Set<String> hosts = new LinkedHashSet<>();
    private final RestClient http;

    public SteamIconFiles(SupabaseStorage storage,
                          @Value("${app.catalog.icon-bucket:achievement-icons}") String bucket,
                          @Value("${app.steam.image-base:https://shared.fastly.steamstatic.com/community_assets/images/apps/}")
                          String imageBase) {
        this.storage = storage;
        this.bucket = bucket;
        this.hosts.add(imageBase.endsWith("/") ? imageBase : imageBase + "/");
        this.hosts.addAll(KNOWN_HOSTS);
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public Optional<IconFile> downloadFromSteam(String iconPath) {
        if (iconPath == null || !ICON_PATH.matcher(iconPath).matches()) {
            return Optional.empty();
        }
        for (String host : hosts) {
            Optional<IconFile> file = fetch(host + iconPath);
            if (file.isPresent()) {
                return file;
            }
        }
        return Optional.empty();
    }

    private Optional<IconFile> fetch(String url) {
        try {
            IconFile file = http.get().uri(url).exchange((request, response) -> {
                String type = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
                if (!response.getStatusCode().is2xxSuccessful() || type == null || !type.startsWith("image/")) {
                    return null;
                }
                byte[] bytes = response.getBody().readNBytes(MAX_BYTES + 1);
                return bytes.length == 0 || bytes.length > MAX_BYTES ? null : new IconFile(bytes, type);
            });
            return Optional.ofNullable(file);
        } catch (RestClientException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public void store(String iconPath, IconFile file) {
        storage.store(bucket, iconPath, file.bytes(), file.contentType());
    }

    @Override
    public void delete(String iconPath) {
        storage.delete(bucket, iconPath);
    }

    @Override
    public String publicUrl(String iconPath) {
        return storage.publicUrl(bucket, iconPath);
    }
}
