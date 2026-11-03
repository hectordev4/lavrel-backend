package com.lavrel.user.adapter.out.persistence.entity;

import com.lavrel.user.domain.Role;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private long publicId;

    @Column(name = "firebase_uid", nullable = false, unique = true)
    private String firebaseUid;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "country_id")
    private Integer countryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserJpaEntity() {
    }

    public UserJpaEntity(Long id, long publicId, String firebaseUid, String username, String email,
                         boolean emailVerified, String photoUrl, Integer countryId, Role role, Instant createdAt) {
        this.id = id;
        this.publicId = publicId;
        this.firebaseUid = firebaseUid;
        this.username = username;
        this.email = email;
        this.emailVerified = emailVerified;
        this.photoUrl = photoUrl;
        this.countryId = countryId;
        this.role = role;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public long getPublicId() {
        return publicId;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public Integer getCountryId() {
        return countryId;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
