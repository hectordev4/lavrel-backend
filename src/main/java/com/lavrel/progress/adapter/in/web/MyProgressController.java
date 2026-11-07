package com.lavrel.progress.adapter.in.web;

import com.lavrel.progress.application.port.in.GetMyProgressUseCase;
import com.lavrel.progress.application.port.in.GetMyProgressUseCase.MyStats;
import com.lavrel.progress.application.port.in.GetMyProgressUseCase.RecentUnlock;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in user's own progress, for the home screen (private: never shown on public profiles):
 *  - GET /api/currentuser/stats                       totals
 *  - GET /api/currentuser/achievements/recent?limit=3 latest unlocks (1 to 20)
 */
@RestController
public class MyProgressController {

    private final GetMyProgressUseCase progress;

    public MyProgressController(GetMyProgressUseCase progress) {
        this.progress = progress;
    }

    @GetMapping("/api/currentuser/stats")
    public MyStats stats(@AuthenticationPrincipal AuthenticatedUser user) {
        return progress.stats(user.userId());
    }

    @GetMapping("/api/currentuser/achievements/recent")
    public RecentResponse recent(@AuthenticationPrincipal AuthenticatedUser user,
                                 @RequestParam(name = "limit", defaultValue = "3")
                                 @Min(1) @Max(GetMyProgressUseCase.MAX_RECENT) int limit) {
        return new RecentResponse(progress.recentUnlocks(user.userId(), limit));
    }

    public record RecentResponse(List<RecentUnlock> achievements) {
    }
}
