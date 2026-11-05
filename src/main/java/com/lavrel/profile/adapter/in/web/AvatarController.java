package com.lavrel.profile.adapter.in.web;

import com.lavrel.profile.application.port.in.ManageAvatarUseCase;
import com.lavrel.profile.application.port.in.ViewProfilesUseCase.Avatar;
import com.lavrel.profile.domain.AvatarGallery;
import com.lavrel.profile.domain.InvalidAvatarException;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import com.lavrel.user.domain.PublicIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * My picture:
 *  - GET    /api/currentuser/profile/avatar
 *  - PUT    /api/currentuser/profile/avatar          body: the image itself (JPG, PNG or GIF), not a form
 *  - PUT    /api/currentuser/profile/avatar/gallery  {"galleryId": "gallery-07"}
 *  - DELETE /api/currentuser/profile/avatar          back to the generated avatar
 * Public: GET /api/avatars/gallery lists the gallery ids. Admin: DELETE /api/admin/users/{publicId}/avatar.
 * Whether others see it is the profile's showAvatar setting.
 */
@RestController
public class AvatarController {

    /** Read at most this much; anything longer is refused before it fills the memory. */
    static final int MAX_UPLOAD_BYTES = 2 * 1024 * 1024;

    private final ManageAvatarUseCase avatars;

    public AvatarController(ManageAvatarUseCase avatars) {
        this.avatars = avatars;
    }

    @GetMapping("/api/currentuser/profile/avatar")
    public Avatar current(@AuthenticationPrincipal AuthenticatedUser user) {
        return avatars.current(user.userId());
    }

    @PutMapping("/api/currentuser/profile/avatar")
    public Avatar upload(@AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request)
            throws IOException {
        return avatars.upload(user.userId(), readLimited(request));
    }

    @PutMapping("/api/currentuser/profile/avatar/gallery")
    public Avatar chooseGallery(@AuthenticationPrincipal AuthenticatedUser user,
                                @Valid @RequestBody GalleryRequest request) {
        return avatars.chooseGallery(user.userId(), request.galleryId());
    }

    @DeleteMapping("/api/currentuser/profile/avatar")
    public Avatar reset(@AuthenticationPrincipal AuthenticatedUser user) {
        return avatars.reset(user.userId());
    }

    @GetMapping("/api/avatars/gallery")
    public GalleryResponse gallery() {
        return new GalleryResponse(AvatarGallery.IDS);
    }

    @DeleteMapping("/api/admin/users/{publicId}/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeByAdmin(@AuthenticationPrincipal AuthenticatedUser admin,
                              @PathVariable("publicId") @Min(PublicIds.MIN) @Max(PublicIds.MAX) long publicId) {
        avatars.removeByAdmin(admin.userId(), publicId);
    }

    static byte[] readLimited(HttpServletRequest request) throws IOException {
        if (request.getContentLengthLong() > MAX_UPLOAD_BYTES) {
            throw new InvalidAvatarException("A picture can be at most 2 MB");
        }
        try (InputStream in = request.getInputStream()) {
            byte[] data = in.readNBytes(MAX_UPLOAD_BYTES + 1);
            if (data.length > MAX_UPLOAD_BYTES) {
                throw new InvalidAvatarException("A picture can be at most 2 MB");
            }
            if (data.length == 0) {
                throw new InvalidAvatarException("Send the picture as the request body");
            }
            return data;
        }
    }

    public record GalleryRequest(@NotBlank String galleryId) {
    }

    public record GalleryResponse(List<String> galleryIds) {
    }
}
