package com.lavrel.user.application.port.in;

import com.lavrel.user.domain.InvalidRoleChangeException;
import com.lavrel.user.domain.Role;
import com.lavrel.user.domain.PublicUserNotFoundException;

public interface ChangeUserRoleUseCase {

    /**
     * Gives the user with that public id a new role. Only admins reach this (SecurityConfig).
     *
     * @param actorUserId the admin making the change
     * @throws PublicUserNotFoundException if nobody has that public id
     * @throws InvalidRoleChangeException  if the admin tries to change their own role
     */
    void changeRole(long actorUserId, long targetPublicId, Role role);
}
