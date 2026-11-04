package com.lavrel.library.application.port.out;

import com.lavrel.library.application.port.in.GetLibraryUseCase.LibraryCriteria;
import com.lavrel.library.application.port.in.GetLibraryUseCase.LibraryPage;

/**
 * Read-only query over the user's owned games. Like the statistics query, it reads tables of other features
 * (platform_account, game, achievement, user_achievement, tracked_game): the documented read-model exception.
 */
public interface LibraryQueryPort {

    LibraryPage find(long userId, LibraryCriteria criteria);
}
