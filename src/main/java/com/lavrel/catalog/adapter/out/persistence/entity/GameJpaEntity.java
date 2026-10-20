package com.lavrel.catalog.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "store_url")
    private String storeUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GameJpaEntity() {
    }

    public GameJpaEntity(Long id, String title, String coverImageUrl, String description,
                         String storeUrl, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.coverImageUrl = coverImageUrl;
        this.description = description;
        this.storeUrl = storeUrl;
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

    public String getDescription() {
        return description;
    }

    public String getStoreUrl() {
        return storeUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
