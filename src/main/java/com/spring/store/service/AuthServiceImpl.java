package com.spring.store.service;

import com.spring.store.dto.ResetPasswordDto;
import com.spring.store.dto.UserRegisterAndLoginRequest;
import com.spring.store.entity.Person;
import com.spring.store.entity.Session;
import com.spring.store.repository.PersonRepository;
import com.spring.store.repository.SessionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional(noRollbackFor = ResponseStatusException.class)
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRepository sessionRepository;

    @Override
    public void registerUser(UserRegisterAndLoginRequest userRegisterRequest) {
        String email = userRegisterRequest.getEmail();
        String password = userRegisterRequest.getPassword();

        boolean userExists = personRepository.existsByEmail(email);
        if (userExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }
        Person person = Person.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .failedAttempt(0)
                .locked(false)
                .build();
        personRepository.save(person);
    }

    @Override
    public String loginUser(UserRegisterAndLoginRequest request) {
        Person person = verifyCredentials(request.getEmail(), request.getPassword());

        String sessionId = UUID.randomUUID().toString();

        Session session = Session.builder()
                .sessionId(sessionId)
                .person(person)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusMinutes(60))
                .build();

        sessionRepository.save(session);

        return sessionId;
    }

    @Override
    public void resetPassword(ResetPasswordDto resetPasswordDto) {
        Person person = verifyCredentials(resetPasswordDto.getEmail(), resetPasswordDto.getPassword());

        person.setPassword(passwordEncoder.encode(resetPasswordDto.getNewPassword()));
        personRepository.save(person);

        sessionRepository.deleteByPerson(person);
    }

    private Person verifyCredentials(String email, String password) {
        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Authentication failed: User with email {} not found", email);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED);
                });

        LocalDateTime now = LocalDateTime.now();

        if (person.isLocked()) {
            if (person.getLastFailedAttempt() != null &&
                    person.getLastFailedAttempt().isBefore(now.minusMinutes(60))) {
                log.info("User lock expired for {}. Unlocking account.", email);
                person.setLocked(false);
                person.setFailedAttempt(0);
            } else {
                log.warn("Authentication blocked: Account is locked for user {}", email);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }
        }

        if (!passwordEncoder.matches(password, person.getPassword())) {
            int newFailedCount = person.getFailedAttempt() + 1;
            log.warn("Authentication failed: Password mismatch for user {}. Failed attempts: {}", email, newFailedCount);

            person.setLastFailedAttempt(now);
            person.setFailedAttempt(newFailedCount);

            if (newFailedCount >= 5) {
                log.warn("User {} has exceeded max attempts. Locking account.", email);
                person.setLocked(true);
            }

            personRepository.save(person);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        person.setFailedAttempt(0);
        person.setLastFailedAttempt(null);
        personRepository.save(person);

        return person;
    }
}
