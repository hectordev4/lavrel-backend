package com.lavrel.library.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.library.application.port.in.GetLibraryUseCase.LibraryCriteria;
import com.lavrel.library.application.port.out.CollectionRepositoryPort;
import com.lavrel.library.application.port.out.LibraryQueryPort;
import com.lavrel.library.domain.CollectionNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetLibraryServiceTest {

    private final LibraryQueryPort query = mock(LibraryQueryPort.class);
    private final CollectionRepositoryPort collections = mock(CollectionRepositoryPort.class);
    private final GetLibraryService service = new GetLibraryService(query, collections);

    @Test
    void browsingSomeoneElsesCollectionIs404() {
        UUID id = UUID.randomUUID();
        when(collections.find(1L, id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.library(1L, new LibraryCriteria(null, null, null, id, null, 0, 48)))
                .isInstanceOf(CollectionNotFoundException.class);
        verify(query, never()).find(anyLong(), any());
    }
}
