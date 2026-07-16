package com.spring.store.service;

import com.spring.store.entity.CartItem;
import com.spring.store.entity.Session;

import java.util.List;
import java.util.Optional;

public interface CartItemService {
    List<CartItem> getItems(Session session);
    CartItem requireItem(Session session, Long productId);
    Optional<CartItem> findItem(Session session, Long productId);
    CartItem saveItem(CartItem item);
    void deleteItem(CartItem item);
    void deleteItems(List<CartItem> items);
}
