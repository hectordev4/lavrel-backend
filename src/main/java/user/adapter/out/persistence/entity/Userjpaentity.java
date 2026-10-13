package user.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firebase_uid", nullable = false, unique = true)
    private String firebaseUid;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "country_id")
    private Integer countryId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserJpaEntity() {
    }

    public UserJpaEntity(Long id, String firebaseUid, String username, String email, String photoUrl, Integer countryId, Instant createdAt) {
        this.id = id;
        this.firebaseUid = firebaseUid;
        this.username = username;
        this.email = email;
        this.photoUrl = photoUrl;
        this.countryId = countryId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
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

    public String getPhotoUrl() {
        return photoUrl;
    }

    public Integer getCountryId() {
        return countryId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
