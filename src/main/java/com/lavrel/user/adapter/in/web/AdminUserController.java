package com.lavrel.user.adapter.in.web;

import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import com.lavrel.user.application.port.in.ChangeUserRoleUseCase;
import com.lavrel.user.domain.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin only (SecurityConfig: /api/admin/** needs role ADMIN). Users are addressed by their public id. */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final ChangeUserRoleUseCase changeUserRole;

    public AdminUserController(ChangeUserRoleUseCase changeUserRole) {
        this.changeUserRole = changeUserRole;
    }

    /** Body {"role": "SUPPORT"} (USER, SUPPORT or ADMIN). 204; 400 invalid_role_change for your own account. */
    @PatchMapping("/{publicId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeRole(@AuthenticationPrincipal AuthenticatedUser admin,
                           @PathVariable("publicId") long publicId,
                           @Valid @RequestBody ChangeRoleRequest request) {
        changeUserRole.changeRole(admin.userId(), publicId, request.role());
    }

    public record ChangeRoleRequest(@NotNull Role role) {
    }
}
