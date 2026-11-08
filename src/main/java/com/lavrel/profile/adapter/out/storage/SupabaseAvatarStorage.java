package com.lavrel.profile.adapter.out.storage;

import com.lavrel.profile.application.port.out.AvatarStoragePort;
import com.lavrel.profile.domain.AvatarStorageUnavailableException;
import com.lavrel.shared.storage.StorageUnavailableException;
import com.lavrel.shared.storage.SupabaseStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Avatars in the public Supabase Storage bucket app.avatars.bucket (default "avatars"). */
@Component
public class SupabaseAvatarStorage implements AvatarStoragePort {

    private final SupabaseStorage storage;
    private final String bucket;

    public SupabaseAvatarStorage(SupabaseStorage storage, @Value("${app.avatars.bucket:avatars}") String bucket) {
        this.storage = storage;
        this.bucket = bucket;
    }

    @Override
    public void store(String path, byte[] bytes, String contentType) {
        try {
            storage.store(bucket, path, bytes, contentType);
        } catch (StorageUnavailableException e) {
            throw new AvatarStorageUnavailableException(e.getMessage(), e);
        }
    }

    @Override
    public void delete(String path) {
        storage.delete(bucket, path);
    }

    @Override
    public String publicUrl(String path) {
        return storage.publicUrl(bucket, path);
    }
}
