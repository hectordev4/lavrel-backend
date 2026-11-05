package com.lavrel.profile.application.port.out;

import com.lavrel.profile.domain.AvatarStorageUnavailableException;

/** Where uploaded pictures live (a public Supabase Storage bucket). */
public interface AvatarStoragePort {

    /** @throws AvatarStorageUnavailableException if storage is not configured or fails */
    void store(String path, byte[] bytes, String contentType);

    /** Best effort: a failure is logged, not thrown (the picture is already unused). */
    void delete(String path);

    /** The address browsers load the picture from. */
    String publicUrl(String path);
}
