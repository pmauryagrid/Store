package com.spring.store.service;

import com.spring.store.entity.CartItem;
import com.spring.store.entity.Session;
import com.spring.store.repository.CartItemRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional
public class CartItemServiceImpl implements CartItemService {

    private final CartItemRepository cartItemRepository;

    @Override
    public List<CartItem> getItems(Session session) {
        return cartItemRepository.findBySessionOrderByIdAsc(session);
    }

    @Override
    public CartItem requireItem(Session session, Long productId) {
        return cartItemRepository.findBySessionAndProductId(session, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
    }

    @Override
    public Optional<CartItem> findItem(Session session, Long productId) {
        return cartItemRepository.findBySessionAndProductId(session, productId);
    }

    @Override
    public CartItem saveItem(CartItem item) {
        return cartItemRepository.save(item);
    }

    @Override
    public void deleteItem(CartItem item) {
        cartItemRepository.delete(item);
    }

    @Override
    public void deleteItems(List<CartItem> items) {
        cartItemRepository.deleteAll(items);
    }
}
