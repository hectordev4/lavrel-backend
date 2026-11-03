package com.lavrel.user.application.service;

import com.lavrel.user.application.port.in.ChangeUserRoleUseCase;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.InvalidRoleChangeException;
import com.lavrel.user.domain.PublicUserNotFoundException;
import com.lavrel.user.domain.Role;
import com.lavrel.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Role changes are logged, so there is a trace of who promoted whom. */
@Service
public class ChangeUserRoleService implements ChangeUserRoleUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChangeUserRoleService.class);

    private final UserRepositoryPort users;

    public ChangeUserRoleService(UserRepositoryPort users) {
        this.users = users;
    }

    @Override
    @Transactional
    public void changeRole(long actorUserId, long targetPublicId, Role role) {
        User target = users.findByPublicId(targetPublicId)
                .orElseThrow(() -> new PublicUserNotFoundException(targetPublicId));
        if (target.id() == actorUserId) {
            throw new InvalidRoleChangeException("You cannot change your own role; ask another admin");
        }
        Role previous = target.role();
        if (!target.changeRole(role)) {
            return;
        }
        users.save(target);
        log.info("User {} changed the role of public id {} from {} to {}", actorUserId, targetPublicId,
                previous, role);
    }
}
