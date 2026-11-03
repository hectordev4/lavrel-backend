package com.lavrel.devtools;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    /** Body {"users": 50, "gamesPerUser": 15}; both optional. Out of range is a 400 validation_failed. */
    @PostMapping
    public CreateResponse create(@Valid @RequestBody(required = false) CreateRequest request) {
        int users = request == null || request.users() == null ? DEFAULT_USERS : request.users();
        int games = request == null || request.gamesPerUser() == null ? DEFAULT_GAMES_PER_USER : request.gamesPerUser();
        FakeDataService.Summary summary = fakeData.create(users, games);
        return new CreateResponse(summary.usersCreated(), summary.gamesAdded(), summary.unlocksRecorded(),
                summary.gamesInCatalog());
    }

    @DeleteMapping
    public DeleteResponse delete() {
        return new DeleteResponse(fakeData.deleteAll());
    }

    public record CreateRequest(@Min(1) @Max(500) Integer users, @Min(1) @Max(100) Integer gamesPerUser) {
    }

    public record CreateResponse(int usersCreated, int gamesAdded, int unlocksRecorded, int gamesInCatalog) {
    }

    public record DeleteResponse(int usersDeleted) {
    }
}
