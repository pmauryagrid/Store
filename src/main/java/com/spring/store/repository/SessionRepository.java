package com.spring.store.repository;

import com.spring.store.entity.Person;
import com.spring.store.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, String> {
    void deleteByPerson(Person person);
}
