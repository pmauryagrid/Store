package com.spring.store.service;

import com.spring.store.dto.CartItemChangeRequest;
import com.spring.store.dto.CartResponse;
import com.spring.store.dto.CheckoutResponse;

public interface CartService {
    CartResponse getCart(String sessionId);

    void addToCart(String sessionId, CartItemChangeRequest request);

    void updateItem(String sessionId, CartItemChangeRequest request);

    void removeItem(String sessionId, Long productId);

    CheckoutResponse checkout(String sessionId);
}
