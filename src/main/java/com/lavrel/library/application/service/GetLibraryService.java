package com.lavrel.library.application.service;

import com.lavrel.library.application.port.in.GetLibraryUseCase;
import com.lavrel.library.application.port.out.LibraryQueryPort;
import org.springframework.stereotype.Service;

@Service
public class GetLibraryService implements GetLibraryUseCase {

    private final LibraryQueryPort libraryQuery;

    public GetLibraryService(LibraryQueryPort libraryQuery) {
        this.libraryQuery = libraryQuery;
    }

    @Override
    public LibraryPage library(long userId, LibraryCriteria criteria) {
        return libraryQuery.find(userId, criteria);
    }
}
