package com.lavrel.catalog.adapter.in.web;

import com.lavrel.catalog.application.port.in.ManageContentExemptionsUseCase;
import com.lavrel.catalog.application.port.in.ManageContentExemptionsUseCase.ExemptGame;
import com.lavrel.catalog.application.port.in.ManageContentExemptionsUseCase.ExemptionResult;
import com.lavrel.catalog.application.port.in.ManageContentExemptionsUseCase.Status;
import com.lavrel.shared.error.ApiError;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin only (/api/admin/**). Content-policy exceptions, by Steam app id:
 *  - POST   /exceptions {"appIds": ["2767030"]}  exempt (and import if needed)
 *  - GET    /exceptions                          list the exempt games
 *  - DELETE /exceptions/{appId}                  remove an exception (the next refresh judges the game again)
 */
@RestController
@RequestMapping("/api/admin/catalog/steam/exceptions")
public class AdminContentExemptionController {

    private static final String APP_ID = "\\d{1,10}";
    private static final String APP_ID_MESSAGE = "must be a numeric Steam app id";

    private final ManageContentExemptionsUseCase exemptions;

    public AdminContentExemptionController(ManageContentExemptionsUseCase exemptions) {
        this.exemptions = exemptions;
    }

    @PostMapping
    public ExemptResponse exempt(@Valid @RequestBody ExemptRequest request) {
        return new ExemptResponse(exemptions.exempt(request.appIds()).stream().map(ResultItem::from).toList());
    }

    @GetMapping
    public ListResponse list() {
        return new ListResponse(exemptions.listExemptions().stream().map(ExemptItem::from).toList());
    }

    @DeleteMapping("/{appId}")
    public ResponseEntity<Object> remove(@PathVariable("appId") @Pattern(regexp = APP_ID, message = APP_ID_MESSAGE)
                                         String appId) {
        if (!exemptions.removeExemption(appId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError("game_not_in_catalog", "Steam app " + appId + " is not in the catalog"));
        }
        return ResponseEntity.noContent().build();
    }

    public record ExemptRequest(
            @NotEmpty @Size(max = 50)
            List<@NotNull @Pattern(regexp = APP_ID, message = APP_ID_MESSAGE) String> appIds) {
    }

    public record ExemptResponse(List<ResultItem> results) {
    }

    public record ResultItem(String appId, Status status, Long gameId, String reason) {

        static ResultItem from(ExemptionResult r) {
            return new ResultItem(r.steamAppId(), r.status(), r.gameId(), r.reason());
        }
    }

    public record ListResponse(List<ExemptItem> exceptions) {
    }

    public record ExemptItem(long gameId, String title, String appId) {

        static ExemptItem from(ExemptGame g) {
            return new ExemptItem(g.gameId(), g.title(), g.steamAppId());
        }
    }
}
