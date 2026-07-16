package com.spring.store.service;

import com.spring.store.entity.Person;
import com.spring.store.entity.Session;
import com.spring.store.repository.SessionRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@Transactional
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;

    @Override
    public Session requireActiveSession(String sessionId) {
        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (session.getExpiredAt() != null && session.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return session;
    }

    @Override
    public void deleteByPerson(Person person) {
        sessionRepository.deleteByPerson(person);
    }
}
