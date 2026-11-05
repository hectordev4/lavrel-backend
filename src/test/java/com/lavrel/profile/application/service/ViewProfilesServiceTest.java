package com.lavrel.profile.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.profile.application.port.out.PublicProfileQueryPort;
import com.lavrel.profile.domain.ProfileNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ViewProfilesServiceTest {

    private final PublicProfileQueryPort query = mock(PublicProfileQueryPort.class);
    private final ViewProfilesService service = new ViewProfilesService(query);

    @Test
    void privateOrUnknownProfilesAreNotFound() {
        when(query.findHeader(1234567890123456L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.header(1234567890123456L)).isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void sixteenDigitsSearchOneExactProfile() {
        service.search(" 1234567890123456 ", 0);
        verify(query).search("1234567890123456", 1234567890123456L, ViewProfilesService.PAGE_SIZE, 0L);
    }

    @Test
    void anythingElseSearchesByNamePageByPage() {
        service.search("otter", 2);
        verify(query).search("otter", null, ViewProfilesService.PAGE_SIZE, 2L * ViewProfilesService.PAGE_SIZE);
    }
}
