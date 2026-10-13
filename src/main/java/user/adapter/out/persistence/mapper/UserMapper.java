package com.yourapp.user.adapter.out.persistence.mapper;

import com.yourapp.user.adapter.out.persistence.entity.UserJpaEntity;
import com.yourapp.user.domain.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static User toDomain(UserJpaEntity e) {
        return new User(e.getId(), e.getFirebaseUid(), e.getUsername(), e.getEmail(), e.getPhotoUrl(),
                e.getCountryId(), e.getCreatedAt());
    }

    public static UserJpaEntity toEntity(User d) {
        return new UserJpaEntity(d.id(), d.firebaseUid(), d.username(), d.email(), d.photoUrl(),
                d.countryId(), d.createdAt());
    }
}
