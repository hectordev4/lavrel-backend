package com.lavrel.profile.application.port.out;

import com.lavrel.profile.domain.InvalidAvatarException;

/** Checks an uploaded picture and prepares the file to store. */
public interface AvatarImagePort {

    /**
     * JPG and PNG: cropped to a square, resized to {@value #SIZE}×{@value #SIZE} and re-encoded, which also drops the
     * hidden photo data (EXIF, including GPS). GIF: kept as it is (animation and transparency included) if it is small
     * enough.
     *
     * @throws InvalidAvatarException if it is not a JPG, PNG or GIF, or breaks the limits
     */
    PreparedAvatar prepare(byte[] upload);

    int SIZE = 256;

    record PreparedAvatar(byte[] bytes, String contentType, String extension) {
    }
}
