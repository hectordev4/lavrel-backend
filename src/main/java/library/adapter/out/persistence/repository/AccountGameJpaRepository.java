package library.adapter.out.persistence.repository;

import library.adapter.out.persistence.entity.AccountGameJpaEntity;
import java.util.List;
import library.adapter.out.persistence.entity.AccountGameId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountGameJpaRepository extends JpaRepository<AccountGameJpaEntity, AccountGameId> {

    List<AccountGameJpaEntity> findByIdPlatformAccountId(long platformAccountId);
}
