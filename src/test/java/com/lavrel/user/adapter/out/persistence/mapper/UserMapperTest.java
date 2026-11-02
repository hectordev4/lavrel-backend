package com.lavrel.user.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavrel.user.domain.PublicIds;
import com.lavrel.user.domain.Role;
import com.lavrel.user.domain.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserMapperTest {

    @Test
    void everyFieldSurvivesTheRoundTrip() {
        User user = User.restore(3L, PublicIds.MIN + 9, "uid-3", "Héctor K.", "me@mail.com", true, "p.png", 4,
                Role.SUPPORT, Instant.parse("2026-11-02T10:00:00Z"));

        User back = UserMapper.toDomain(UserMapper.toEntity(user));

        assertThat(back).usingRecursiveComparison().isEqualTo(user);
    }
}
