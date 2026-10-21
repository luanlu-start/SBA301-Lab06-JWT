package fu.se.lab06jwt.services;

import fu.se.lab06jwt.dtos.LoginUserDto;
import fu.se.lab06jwt.dtos.RegisterUserDto;
import fu.se.lab06jwt.entities.User;
import fu.se.lab06jwt.exceptions.DuplicateEmailException;
import fu.se.lab06jwt.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public User signup(RegisterUserDto input) {
        String email = normalizeEmail(input.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        User user = new User(
                input.getFullName().trim(),
                email,
                passwordEncoder.encode(input.getPassword())
        );
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }

    public User authenticate(LoginUserDto input) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizeEmail(input.getEmail()),
                        input.getPassword()
                )
        );
        return (User) authentication.getPrincipal();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
