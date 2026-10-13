package catalog.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "game")
public class GameJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GameJpaEntity() {
    }

    public GameJpaEntity(Long id, String title, String coverImageUrl, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.coverImageUrl = coverImageUrl;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
