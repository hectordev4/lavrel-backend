package catalog.adapter.out.persistence.repository;

import catalog.adapter.out.persistence.entity.GameJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameJpaRepository extends JpaRepository<GameJpaEntity, Long> {
}
