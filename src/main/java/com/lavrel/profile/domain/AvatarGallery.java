package com.lavrel.profile.domain;

import java.util.List;
import java.util.stream.IntStream;

/**
 * The ids of the default pictures users can pick: gallery-01 to gallery-36. The images themselves are in the frontend
 * (CC0 art, e.g. from Kenney.nl); the backend only checks that a chosen id exists.
 */
public final class AvatarGallery {

    public static final int SIZE = 36;
    public static final List<String> IDS =
            IntStream.rangeClosed(1, SIZE).mapToObj(i -> "gallery-%02d".formatted(i)).toList();

    private AvatarGallery() {
    }

    public static boolean contains(String id) {
        return IDS.contains(id);
    }
}
