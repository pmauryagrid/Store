package com.spring.store.repository;

import com.spring.store.entity.CartItem;
import com.spring.store.entity.Product;
import com.spring.store.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findBySessionAndProduct(Session session, Product product);
    Optional<CartItem> findBySessionAndProductId(Session session, Long productId);
    List<CartItem> findBySessionOrderByIdAsc(Session session);
}
