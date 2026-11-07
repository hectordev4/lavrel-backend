package com.lavrel.progress.application.service;

import com.lavrel.progress.application.port.in.GetMyProgressUseCase;
import com.lavrel.progress.application.port.out.MyProgressQueryPort;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetMyProgressService implements GetMyProgressUseCase {

    private final MyProgressQueryPort query;

    public GetMyProgressService(MyProgressQueryPort query) {
        this.query = query;
    }

    @Override
    public MyStats stats(long userId) {
        return query.stats(userId);
    }

    @Override
    public List<RecentUnlock> recentUnlocks(long userId, int limit) {
        return query.recentUnlocks(userId, Math.max(1, Math.min(limit, MAX_RECENT)));
    }
}
