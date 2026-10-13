package user.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user.application.port.in.GetUserCountryUseCase;
import user.application.port.out.UserRepositoryPort;
import user.domain.UserNotFoundException;

@Service
@Transactional(readOnly = true)
public class GetUserCountryService implements GetUserCountryUseCase {

    private final UserRepositoryPort users;

    public GetUserCountryService(UserRepositoryPort users) {
        this.users = users;
    }

    @Override
    public Integer getCountryId(long userId) {
        return users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId)).countryId();
    }
}
