package com.lavrel.library.application.service;

import com.lavrel.library.application.port.in.GetLibraryUseCase;
import com.lavrel.library.application.port.out.CollectionRepositoryPort;
import com.lavrel.library.application.port.out.LibraryQueryPort;
import com.lavrel.library.domain.CollectionNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetLibraryService implements GetLibraryUseCase {

    private final LibraryQueryPort libraryQuery;
    private final CollectionRepositoryPort collections;

    public GetLibraryService(LibraryQueryPort libraryQuery, CollectionRepositoryPort collections) {
        this.libraryQuery = libraryQuery;
        this.collections = collections;
    }

    @Override
    public LibraryPage library(long userId, LibraryCriteria criteria) {
        if (criteria.collectionId() != null && collections.find(userId, criteria.collectionId()).isEmpty()) {
            throw new CollectionNotFoundException();
        }
        return libraryQuery.find(userId, criteria);
    }
}
