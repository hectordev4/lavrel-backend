package com.lavrel.catalog.application.port.out;

import java.util.Optional;

/** Our own copies of achievement icons (Supabase Storage, bucket app.catalog.icon-bucket). */
public interface IconFilesPort {

    /** Downloads the icon from Steam, trying its known image hosts. Empty if no host has it. */
    Optional<IconFile> downloadFromSteam(String iconPath);

    /** @throws com.lavrel.shared.storage.StorageUnavailableException if storage is not configured or refuses */
    void store(String iconPath, IconFile file);

    void delete(String iconPath);

    String publicUrl(String iconPath);

    record IconFile(byte[] bytes, String contentType) {
    }
}
