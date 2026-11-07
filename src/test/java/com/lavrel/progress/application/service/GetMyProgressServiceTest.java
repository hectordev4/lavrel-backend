package com.lavrel.progress.application.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.lavrel.progress.application.port.in.GetMyProgressUseCase;
import com.lavrel.progress.application.port.out.MyProgressQueryPort;
import org.junit.jupiter.api.Test;

class GetMyProgressServiceTest {

    private final MyProgressQueryPort query = mock(MyProgressQueryPort.class);
    private final GetMyProgressService service = new GetMyProgressService(query);

    @Test
    void theNumberOfRecentUnlocksIsKeptInRange() {
        service.recentUnlocks(1L, 0);
        verify(query).recentUnlocks(1L, 1);

        service.recentUnlocks(2L, 500);
        verify(query).recentUnlocks(2L, GetMyProgressUseCase.MAX_RECENT);
    }
}
