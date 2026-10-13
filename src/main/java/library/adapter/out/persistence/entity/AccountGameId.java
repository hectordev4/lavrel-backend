package library.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public record AccountGameId(
        @Column(name = "platform_account_id", nullable = false) long platformAccountId,
        @Column(name = "game_id", nullable = false) long gameId) implements Serializable {
}
