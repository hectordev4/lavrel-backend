package com.lavrel.devtools;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DEVELOPMENT ONLY and admin only. Not even registered unless app.dev.fake-data-enabled=true.
 */
@RestController
@RequestMapping("/api/admin/dev/fake-data")
@ConditionalOnProperty(name = "app.dev.fake-data-enabled", havingValue = "true")
public class FakeDataController {

    private static final int DEFAULT_USERS = 50;
    private static final int DEFAULT_GAMES_PER_USER = 15;

    private final FakeDataService fakeData;

    public FakeDataController(FakeDataService fakeData) {
        this.fakeData = fakeData;
    }

    /** Body {"users": 50, "gamesPerUser": 15}; both optional. */
    @PostMapping
    public FakeDataService.Summary create(@RequestBody(required = false) CreateRequest request) {
        int users = request == null || request.users() == null ? DEFAULT_USERS : request.users();
        int games = request == null || request.gamesPerUser() == null ? DEFAULT_GAMES_PER_USER : request.gamesPerUser();
        return fakeData.create(Math.max(1, Math.min(users, 500)), Math.max(1, Math.min(games, 100)));
    }

    @DeleteMapping
    public DeleteResponse delete() {
        return new DeleteResponse(fakeData.deleteAll());
    }

    public record CreateRequest(Integer users, Integer gamesPerUser) {
    }

    public record DeleteResponse(int usersDeleted) {
    }
}
