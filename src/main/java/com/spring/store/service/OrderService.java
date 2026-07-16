package com.spring.store.service;

import com.spring.store.dto.CheckoutResponse;
import com.spring.store.dto.OrderResponse;

import java.util.List;

public interface OrderService {
    CheckoutResponse checkout(String sessionId);
    List<OrderResponse> getOrders(String sessionId);
    void cancelOrder(String sessionId, Long orderId);
}
