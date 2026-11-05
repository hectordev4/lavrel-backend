package com.lavrel.catalog.adapter.in.web;

import com.lavrel.catalog.application.port.in.ManageGameSystemsUseCase;
import com.lavrel.catalog.domain.GameSystem;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin only. Which systems a game is available on ({system}: pc, xbox, playstation):
 *  - PUT    /api/admin/catalog/games/{gameId}/systems/{system}   mark it (204)
 *  - DELETE /api/admin/catalog/games/{gameId}/systems/{system}   unmark it (204)
 */
@RestController
@RequestMapping("/api/admin/catalog/games/{gameId}/systems/{system}")
public class AdminGameSystemsController {

    private final ManageGameSystemsUseCase systems;

    public AdminGameSystemsController(ManageGameSystemsUseCase systems) {
        this.systems = systems;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@PathVariable("gameId") @Positive long gameId, @PathVariable("system") GameSystem system) {
        systems.add(gameId, system);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable("gameId") @Positive long gameId, @PathVariable("system") GameSystem system) {
        systems.remove(gameId, system);
    }
}
