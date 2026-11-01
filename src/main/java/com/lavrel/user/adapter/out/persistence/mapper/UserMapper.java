package com.lavrel.user.adapter.out.persistence.mapper;

import com.lavrel.user.adapter.out.persistence.entity.UserJpaEntity;
import com.lavrel.user.domain.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static User toDomain(UserJpaEntity e) {
        return new User(e.getId(), e.getPublicId(), e.getFirebaseUid(), e.getUsername(), e.getEmail(),
                e.isEmailVerified(), e.getPhotoUrl(), e.getCountryId(), e.getRole(), e.getCreatedAt());
    }

    public static UserJpaEntity toEntity(User d) {
        return new UserJpaEntity(d.id(), d.publicId(), d.firebaseUid(), d.username(), d.email(), d.emailVerified(),
                d.photoUrl(), d.countryId(), d.role(), d.createdAt());
    }
}
