package com.lavrel.progress.application.port.out;

import com.lavrel.progress.application.port.in.GetMyProgressUseCase.MyStats;
import com.lavrel.progress.application.port.in.GetMyProgressUseCase.RecentUnlock;
import java.util.List;

/** Reads unlocks, achievements, games and the user's accounts: the documented read-model exception. */
public interface MyProgressQueryPort {

    MyStats stats(long userId);

    List<RecentUnlock> recentUnlocks(long userId, int limit);
}
