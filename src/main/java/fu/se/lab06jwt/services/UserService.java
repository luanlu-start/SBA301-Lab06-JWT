package fu.se.lab06jwt.services;

import fu.se.lab06jwt.entities.User;
import fu.se.lab06jwt.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<User> allUsers() {
        return userRepository.findAll();
    }
}
