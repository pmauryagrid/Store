package com.spring.store.service;

import com.spring.store.dto.ResetPasswordDto;
import com.spring.store.dto.UserRegisterAndLoginRequest;
import com.spring.store.entity.Person;
import com.spring.store.entity.Session;
import com.spring.store.repository.PersonRepository;
import com.spring.store.repository.SessionRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
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
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "User already exists");
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        LocalDateTime now = LocalDateTime.now();

        if (person.isLocked()) {
            if (person.getLastFailedAttempt() != null &&
                    person.getLastFailedAttempt().isBefore(now.minusMinutes(60))) {
                person.setLocked(false);
                person.setFailedAttempt(0);
            } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }
        }

        if (!passwordEncoder.matches(password, person.getPassword())) {
            person.setLastFailedAttempt(now);
            person.setFailedAttempt(person.getFailedAttempt() + 1);

            if (person.getFailedAttempt() >= 5) {
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
