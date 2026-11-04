package com.lavrel.library.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavrel.library.application.port.in.GetLibraryUseCase.LibraryCriteria;
import com.lavrel.library.application.port.in.GetLibraryUseCase.Sort;
import org.junit.jupiter.api.Test;

class LibraryCriteriaTest {

    @Test
    void defaultsToRecentlyPlayedAndAFullGrid() {
        LibraryCriteria c = new LibraryCriteria("  ", null, null, null, -1, 0);
        assertThat(c.query()).isNull();
        assertThat(c.sort()).isEqualTo(Sort.RECENT);
        assertThat(c.page()).isZero();
        assertThat(c.size()).isEqualTo(LibraryCriteria.DEFAULT_SIZE);
    }

    @Test
    void sizeIsCapped() {
        assertThat(new LibraryCriteria(null, null, null, Sort.NAME, 0, 1000).size()).isEqualTo(LibraryCriteria.MAX_SIZE);
    }
}
