package com.spring.store.service;

import com.spring.store.entity.Person;
import com.spring.store.entity.Session;

public interface SessionService {
    Session requireActiveSession(String sessionId);
    void deleteByPerson(Person person);
}
