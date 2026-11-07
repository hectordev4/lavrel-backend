package com.lavrel.catalog.adapter.in.web;

import com.lavrel.catalog.application.port.in.ManageFranchisesUseCase;
import com.lavrel.catalog.application.port.in.ManageFranchisesUseCase.CleanupReport;
import com.lavrel.catalog.application.port.out.FranchiseBlocklistPort.BlockedFranchise;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin only. Keeping the franchise filter clean:
 *  - POST   /api/admin/catalog/franchises/cleanup?apply=false   what the rules reject (apply=true removes it)
 *  - PUT    /api/admin/catalog/franchises/{creatorId}/blocked   "this is not a franchise" (ids from /api/creators)
 *  - DELETE /api/admin/catalog/franchises/{creatorId}/blocked   undo
 *  - GET    /api/admin/catalog/franchises/blocked
 */
@RestController
@RequestMapping("/api/admin/catalog/franchises")
public class AdminFranchisesController {

    private final ManageFranchisesUseCase franchises;

    public AdminFranchisesController(ManageFranchisesUseCase franchises) {
        this.franchises = franchises;
    }

    @PostMapping("/cleanup")
    public CleanupReport cleanup(@RequestParam(name = "apply", defaultValue = "false") boolean apply) {
        return franchises.cleanup(apply);
    }

    @PutMapping("/{creatorId}/blocked")
    public BlockedFranchise block(@PathVariable("creatorId") @Positive long creatorId) {
        return franchises.block(creatorId);
    }

    @DeleteMapping("/{creatorId}/blocked")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@PathVariable("creatorId") @Positive long creatorId) {
        franchises.unblock(creatorId);
    }

    @GetMapping("/blocked")
    public BlockedResponse blocked() {
        return new BlockedResponse(franchises.blocked());
    }

    public record BlockedResponse(List<BlockedFranchise> blocked) {
    }
}
