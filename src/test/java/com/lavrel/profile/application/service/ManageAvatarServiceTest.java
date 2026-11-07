package com.lavrel.profile.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.profile.application.port.out.AvatarImagePort;
import com.lavrel.profile.application.port.out.AvatarImagePort.PreparedAvatar;
import com.lavrel.profile.application.port.out.AvatarStoragePort;
import com.lavrel.profile.application.port.out.ProfileSettingsRepositoryPort;
import com.lavrel.profile.application.port.out.PublicProfileQueryPort;
import com.lavrel.profile.domain.AvatarKind;
import com.lavrel.profile.domain.AvatarStorageUnavailableException;
import com.lavrel.profile.domain.InvalidAvatarException;
import com.lavrel.profile.domain.ProfileSettings;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ManageAvatarServiceTest {

    private final ProfileSettingsRepositoryPort settings = mock(ProfileSettingsRepositoryPort.class);
    private final AvatarImagePort images = mock(AvatarImagePort.class);
    private final AvatarStoragePort storage = mock(AvatarStoragePort.class);
    private final PublicProfileQueryPort users = mock(PublicProfileQueryPort.class);
    private final ManageAvatarService service = new ManageAvatarService(settings, images, storage, users);

    private static final byte[] PICTURE = {1, 2, 3};

    @Test
    void uploadStoresThenSavesThenDeletesTheOldOne() {
        ProfileSettings s = ProfileSettings.restore(1L, true, false, false, true, false, AvatarKind.UPLOADED, "old.png");
        when(settings.find(1L)).thenReturn(Optional.of(s));
        when(images.prepare(PICTURE)).thenReturn(new PreparedAvatar(new byte[] {9}, "image/png", "png"));
        when(storage.publicUrl(anyString())).thenAnswer(i -> "https://cdn/" + i.getArgument(0));

        var avatar = service.upload(1L, PICTURE);

        InOrder order = inOrder(storage, settings);
        order.verify(storage).store(anyString(), any(), eq("image/png"));
        order.verify(settings).save(s);
        order.verify(storage).delete("old.png");
        assertThat(avatar.kind()).isEqualTo(AvatarKind.UPLOADED);
        assertThat(s.avatarValue()).endsWith(".png").isNotEqualTo("old.png");
    }

    @Test
    void storageTroubleChangesNothing() {
        when(settings.find(1L)).thenReturn(Optional.empty());
        when(images.prepare(PICTURE)).thenReturn(new PreparedAvatar(new byte[] {9}, "image/gif", "gif"));
        doThrow(new AvatarStorageUnavailableException("down", null)).when(storage)
                .store(anyString(), any(), anyString());

        assertThatThrownBy(() -> service.upload(1L, PICTURE)).isInstanceOf(AvatarStorageUnavailableException.class);
        verify(settings, never()).save(any());
    }

    @Test
    void onlyGalleryIdsThatExist() {
        when(settings.find(1L)).thenReturn(Optional.empty());

        assertThat(service.chooseGallery(1L, "gallery-07").galleryId()).isEqualTo("gallery-07");
        assertThatThrownBy(() -> service.chooseGallery(1L, "mario")).isInstanceOf(InvalidAvatarException.class);
    }

    @Test
    void resetDeletesTheUpload() {
        ProfileSettings s = ProfileSettings.restore(1L, true, false, false, true, false, AvatarKind.UPLOADED, "pic.gif");
        when(settings.find(1L)).thenReturn(Optional.of(s));

        assertThat(service.reset(1L).kind()).isEqualTo(AvatarKind.GENERATED);
        verify(storage).delete("pic.gif");
    }
}
