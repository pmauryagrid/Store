package com.spring.store.repository;

import com.spring.store.entity.Order;
import com.spring.store.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByPersonOrderByCreatedAtDesc(Person person);
}
