package user.application.port.out;

import java.util.List;
import java.util.Optional;
import user.domain.Country;

public interface CountryRepositoryPort {

    Optional<Country> findById(int id);

    List<Country> findAll();

    boolean existsById(int id);
}
